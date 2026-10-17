package dev.aaa1115910.biliapi.http.util

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.setCookie
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 维护 Web 侧的指纹与票据 cookie：buvid3/buvid4/b_nut、bili_ticket，
 * 以及基于 refresh_token 的 cookie 每日续期。
 *
 * 账号凭证、cookie 的持久化方式由应用层通过 provider 注入，本类不直接依赖存储。
 */
@OptIn(ExperimentalEncodingApi::class)
object WebCookieManager {
    private val logger = KotlinLogging.logger { }

    private const val BILI_TICKET_KEY_ID = "ec02"
    private const val BILI_TICKET_HMAC_KEY = "XgwSnGZ1p"

    private const val REFRESH_SOURCE = "main_web"

    private val refreshCsrfRegex = Regex("<div\\s+id=\"1-name\">\\s*([0-9a-fA-F]{16,})\\s*</div>")

    // correspond 接口的固定公钥，来自 bilibili-api-docs 的 cookie_refresh.md
    private val correspondPublicKey by lazy {
        val derBase64 =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLgd2OAkcGVtoE3ThUREbio0Eg" +
                "Uc/prcajMKXvkCKFCWhJYJcLkcM2DKKcSeFpD/j6Boy538YXnR6VhcuUJOhH2x71" +
                "nzPjfdTcqMz7djHum0qSZA0AyCBDABUqCrfNgCiJ00Ra7GmRj+YCK1NJEuewlb40" +
                "JNrRuoEUXpabUzGB8QIDAQAB"
        val keyBytes = Base64.decode(derBase64)
        KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(keyBytes))
    }

    // ---- 由应用层注入 ----
    /** 当前登录凭证：SESSDATA、bili_jct、DedeUserID、refresh_token 等 */
    var sessDataProvider: () -> String = { "" }
    var biliJctProvider: () -> String = { "" }
    var refreshTokenProvider: () -> String = { "" }
    var midProvider: () -> Long = { 0 }
    var uidCkMd5Provider: () -> String = { "" }
    var sidProvider: () -> String = { "" }

    /** 读取指定 cookie 的值 */
    var cookieGetter: (name: String) -> String? = { null }

    /** 请求头实际使用的 buvid3，用于保证它与 buvid4 由同一次 finger/spi 签发 */
    var appBuvid3Provider: () -> String? = { null }

    /** 新增/更新的 cookie 回调 */
    var onCookiesUpdated: (Map<String, String>) -> Unit = {}

    // 进程内日期标记：重启后当天最多再尝试一次
    private var biliTicketCheckedEpochDay = 0L
    private var buvidActiveEpochDay = 0L
    private var cookieRefreshCheckedEpochDay = 0L

    private val ticketMutex = Mutex()
    private val fingerprintMutex = Mutex()
    private val cookieRefreshMutex = Mutex()

    private val json = Json { ignoreUnknownKeys = true }

    // 只用于抓 Set-Cookie 和调用指纹接口。不挂 BiliUserAgent：这些是 Web 接口，
    // App UA 会增加指纹不一致的风险
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000L
                connectTimeoutMillis = 10_000L
                socketTimeoutMillis = 20_000L
            }
        }
    }

    /**
     * 启动时在后台调用一次，各步骤互不影响，失败不影响应用启动。
     */
    suspend fun initialize() {
        runCatching { ensureWebFingerprintCookies() }
            .onFailure { logger.warn(it) { "ensureWebFingerprintCookies failed" } }
        runCatching { ensureBiliTicket() }
            .onFailure { logger.warn(it) { "ensureBiliTicket failed" } }
        runCatching { ensureBuvidActiveOncePerDay() }
            .onFailure { logger.warn(it) { "ensureBuvidActiveOncePerDay failed" } }
        runCatching { refreshCookieIfNeededOncePerDay() }
            .onFailure { logger.warn(it) { "refreshCookieIfNeededOncePerDay failed" } }
    }

    /** 汇总 Web 指纹 cookie，供 API 层附加到请求头 */
    fun cookieHeader(): String? {
        val parts = buildList {
            listOf("buvid3", "buvid4", "b_nut", "bili_ticket", "bili_ticket_expires")
                .forEach { name ->
                    cookieGetter(name)?.takeIf { it.isNotBlank() }?.let { add("$name=$it") }
                }
        }
        return if (parts.isEmpty()) null else parts.joinToString("; ") + ";"
    }

    /**
     * 账号凭证 + Web 指纹 cookie 拼成的完整 Cookie 头。
     *
     * passport 与风控接口靠 Cookie 识别用户；csrf 参数只有配合会话 cookie 才有意义。
     * [overrides] 里的同名 cookie 优先，用于 cookie/refresh 之后拿新签发的值去 confirm。
     */
    private fun fullCookieHeader(overrides: Map<String, String> = emptyMap()): String? {
        val parts = mutableListOf<String>()
        fun add(name: String, value: String?) {
            value?.takeIf { it.isNotBlank() }?.let { parts += "$name=$it" }
        }
        add("SESSDATA", overrides["SESSDATA"] ?: sessDataProvider())
        add("bili_jct", overrides["bili_jct"] ?: biliJctProvider())
        add("DedeUserID", overrides["DedeUserID"] ?: midProvider().takeIf { it > 0 }?.toString())
        add("DedeUserID__ckMd5", overrides["DedeUserID__ckMd5"] ?: uidCkMd5Provider())
        add("sid", overrides["sid"] ?: sidProvider())
        cookieHeader()?.split(";")?.map { it.trim() }?.filter { it.isNotBlank() }?.forEach { part ->
            val name = part.substringBefore("=")
            if (parts.none { it.startsWith("$name=") }) parts += part
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    private fun HttpRequestBuilder.withAuthCookies(overrides: Map<String, String> = emptyMap()) {
        fullCookieHeader(overrides)?.let { header(HttpHeaders.Cookie, it) }
    }

    /**
     * 确保 buvid3/buvid4/b_nut 就绪：先访问首页拿 b_nut（可能附带 buvid3），
     * 再走 finger/spi 拿成对的 b_3/b_4。
     *
     * buvid3 与 buvid4 必须同源，否则风控会认为两个值来自不同设备。请求头里用的是
     * [appBuvid3Provider]，所以只有它与 Web 侧 buvid3 一致时才认为指纹就绪。
     */
    suspend fun ensureWebFingerprintCookies() = fingerprintMutex.withLock {
        val webBuvid3 = cookieGetter("buvid3")?.takeIf { it.isNotBlank() }
        val appBuvid3 = appBuvid3Provider()?.trim().orEmpty()
        val needHomepage = webBuvid3 == null || cookieGetter("b_nut").isNullOrBlank()
        val needSpi = cookieGetter("buvid4").isNullOrBlank() || webBuvid3 != appBuvid3
        if (!needHomepage && !needSpi) return

        if (needHomepage) {
            runCatching {
                val setCookies = client.get("https://www.bilibili.com/").setCookie()
                val updated = mutableMapOf<String, String>()
                for (cookie in setCookies) {
                    if (cookie.name != "buvid3" && cookie.name != "b_nut") continue
                    // 已有 Web 侧 buvid3 时不用首页值覆盖，否则会破坏 finger/spi 的成对指纹
                    if (cookie.name == "buvid3" && webBuvid3 != null) continue
                    cookie.value.takeIf { it.isNotBlank() }?.let { updated[cookie.name] = it }
                }
                if (updated.isNotEmpty()) onCookiesUpdated(updated)
            }.onFailure {
                logger.warn(it) { "fetch homepage cookies failed" }
            }
        }

        if (needSpi) {
            runCatching {
                val raw = client.get("https://api.bilibili.com/x/frontend/finger/spi").bodyAsText()
                val data = json.parseToJsonElement(raw).jsonObject
                    .get("data")?.jsonObject ?: buildJsonObject { }
                val b3 = data["b_3"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val b4 = data["b_4"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val updated = mutableMapOf<String, String>()
                // b_3/b_4 成对签发，buvid3 一律以它为权威值
                if (b3.isNotBlank()) updated["buvid3"] = b3
                if (b4.isNotBlank()) updated["buvid4"] = b4
                if (updated.isNotEmpty()) onCookiesUpdated(updated)
            }.onFailure {
                logger.warn(it) { "fetch finger/spi failed" }
            }
        }
    }

    /**
     * 获取/续期 bili_ticket。剩余有效期超过 6 小时或当天已尝试过则跳过。
     */
    suspend fun ensureBiliTicket() {
        val nowMs = System.currentTimeMillis()
        val epochDay = nowMs / 86_400_000L
        if (biliTicketCheckedEpochDay == epochDay) return
        val existing = cookieGetter("bili_ticket")
        val existingExpires = cookieGetter("bili_ticket_expires")?.toLongOrNull() ?: 0L
        if (!existing.isNullOrBlank() && existingExpires * 1000L - nowMs > 6 * 60 * 60 * 1000L) {
            return
        }
        biliTicketCheckedEpochDay = epochDay

        ticketMutex.withLock {
            runCatching {
                val ts = (nowMs / 1000).toString()
                val hexsign = hmacSha256Hex(key = BILI_TICKET_HMAC_KEY, message = "ts$ts")
                val csrf = biliJctProvider().takeIf { it.isNotBlank() }
                val raw = client.post("https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket") {
                    header(HttpHeaders.ContentType, "application/x-www-form-urlencoded; charset=utf-8")
                    parameter("key_id", BILI_TICKET_KEY_ID)
                    parameter("hexsign", hexsign)
                    parameter("context[ts]", ts)
                    csrf?.let { parameter("csrf", it) }
                    withAuthCookies()
                }.bodyAsText()
                val data = json.parseToJsonElement(raw).jsonObject
                    .get("data")?.jsonObject ?: return@runCatching
                val ticket = data["ticket"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val createdAt = data["created_at"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                val ttl = data["ttl"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                if (ticket.isBlank() || createdAt <= 0L || ttl <= 0L) return@runCatching
                onCookiesUpdated(
                    mapOf(
                        "bili_ticket" to ticket,
                        "bili_ticket_expires" to (createdAt + ttl).toString()
                    )
                )
            }.onFailure {
                logger.warn(it) { "GenWebTicket failed" }
            }
        }
    }

    /**
     * 每天调用一次 ExClimbWuzhi 激活 buvid，缓解风控。
     */
    suspend fun ensureBuvidActiveOncePerDay() {
        val mid = midProvider()
        if (mid <= 0) return
        val epochDay = System.currentTimeMillis() / 86_400_000L
        if (buvidActiveEpochDay == epochDay) return

        runCatching {
            val rand = ByteArray(32 + 8 + 4)
            SecureRandom().nextBytes(rand)
            // 模拟 PNG 的 IEND 结尾标记
            byteArrayOf(0, 0, 0, 0, 73, 69, 78, 68).copyInto(rand, 32)
            val tail = ByteArray(4)
            SecureRandom().nextBytes(tail)
            tail.copyInto(rand, 40)

            val randPngEnd = Base64.encode(rand)
            val payload = buildJsonObject {
                put("3064", 1)
                put("39c8", "333.1387.fp.risk")
                put(
                    "3c43",
                    buildJsonObject {
                        put("adca", "Linux")
                        put("bfe9", randPngEnd.takeLast(50))
                    }
                )
            }

            client.post("https://api.bilibili.com/x/internal/gaia-gateway/ExClimbWuzhi") {
                header(HttpHeaders.ContentType, "application/json")
                header("env", "prod")
                header("app-key", "android64")
                header("x-bili-aurora-zone", "sh001")
                header("x-bili-mid", mid.toString())
                genAuroraEid(mid)?.let { header("x-bili-aurora-eid", it) }
                header("Referer", "https://www.bilibili.com")
                withAuthCookies()
                setBody(
                    TextContent(
                        buildJsonObject { put("payload", payload) }.toString(),
                        ContentType.Application.Json
                    )
                )
            }
            buvidActiveEpochDay = epochDay
        }.onFailure {
            logger.warn(it) { "ExClimbWuzhi failed, mid=$mid" }
        }
    }

    private fun genAuroraEid(mid: Long): String? {
        if (mid <= 0) return null
        val key = "ad1va46a7lza".toByteArray()
        val input = mid.toString().toByteArray()
        val out = ByteArray(input.size)
        for (i in input.indices) out[i] = (input[i].toInt() xor key[i % key.size].toInt()).toByte()
        return Base64.encode(out).trimEnd('=')
    }

    /**
     * 基于 refresh_token 的 Web Cookie 每日续期，成功后把新 cookie 回调给应用层。
     */
    suspend fun refreshCookieIfNeededOncePerDay() {
        if (sessDataProvider().isBlank() || biliJctProvider().isBlank()) return
        val refreshToken = refreshTokenProvider().takeIf { it.isNotBlank() } ?: return
        val epochDay = System.currentTimeMillis() / 86_400_000L
        if (cookieRefreshCheckedEpochDay == epochDay) return

        cookieRefreshMutex.withLock {
            if (cookieRefreshCheckedEpochDay == epochDay) return
            runCatching {
                val info = client.get("https://passport.bilibili.com/x/passport-login/web/cookie/info") {
                    withAuthCookies()
                    url { parameters.append("csrf", biliJctProvider()) }
                }.bodyAsText()
                val infoData = json.parseToJsonElement(info).jsonObject["data"]?.jsonObject
                    ?: return@runCatching
                val shouldRefresh = infoData["refresh"]?.jsonPrimitive?.contentOrNull == "true" ||
                    infoData["refresh"]?.jsonPrimitive?.booleanOrNull == true
                if (!shouldRefresh) return@runCatching

                val timestamp = infoData["timestamp"]?.jsonPrimitive?.contentOrNull
                    ?.toLongOrNull() ?: System.currentTimeMillis()
                val correspondPath = getCorrespondPath(timestamp)
                val html = client.get("https://www.bilibili.com/correspond/1/$correspondPath") {
                    withAuthCookies()
                }.bodyAsText()
                val refreshCsrf = refreshCsrfRegex.find(html)?.groupValues?.getOrNull(1).orEmpty()
                if (refreshCsrf.isBlank()) error("refresh_csrf not found")

                val raw = client.get("https://passport.bilibili.com/x/passport-login/web/cookie/refresh") {
                    withAuthCookies()
                    parameter("csrf", biliJctProvider())
                    parameter("refresh_csrf", refreshCsrf)
                    parameter("source", REFRESH_SOURCE)
                    parameter("refresh_token", refreshToken)
                }.bodyAsText()
                val root = json.parseToJsonElement(raw).jsonObject
                val data = root["data"]?.jsonObject
                if (root["code"]?.jsonPrimitive?.contentOrNull != "0" || data == null) {
                    error("cookie refresh failed: ${root["message"]?.jsonPrimitive?.contentOrNull}")
                }
                val refreshed = mutableMapOf<String, String>()
                data["cookies"]?.jsonArray?.forEach { element ->
                    val cookie = element.jsonObject
                    val name = cookie["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    val value = cookie["value"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    refreshed[name] = value
                }
                data["refresh_token"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }?.let { refreshed["refresh_token"] = it }
                if (refreshed.isNotEmpty()) onCookiesUpdated(refreshed)

                // 应用层可能异步落盘，confirm 直接使用刚拿到的值
                val newBiliJct = refreshed["bili_jct"]?.takeIf { it.isNotBlank() }
                    ?: biliJctProvider()
                runCatching {
                    client.get("https://passport.bilibili.com/x/passport-login/web/confirm/refresh") {
                        withAuthCookies(refreshed)
                        parameter("csrf", newBiliJct)
                        parameter("refresh_token", refreshToken)
                    }
                }
                cookieRefreshCheckedEpochDay = epochDay
            }.onFailure {
                logger.warn(it) { "cookie refresh failed" }
            }
        }
    }

    /**
     * correspond 路径：RSA OAEP-SHA256 加密 "refresh_${timestamp}" 后转十六进制。
     */
    private fun getCorrespondPath(timestampMs: Long): String {
        val plaintext = "refresh_$timestampMs"
        val cipher =
            runCatching { Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding") }
                .getOrElse { Cipher.getInstance("RSA/ECB/OAEPPadding") }
        cipher.init(
            Cipher.ENCRYPT_MODE,
            correspondPublicKey,
            OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
            )
        )
        return cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256Hex(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
