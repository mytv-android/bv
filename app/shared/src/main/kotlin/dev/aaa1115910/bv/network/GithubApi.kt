package dev.aaa1115910.bv.network

import android.os.Build
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.util.Prefs
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GithubApiException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null,
) : Exception(message, cause)

object GithubApi {
    private const val API_BASE = "https://api.github.com"
    private const val OWNER = "mytv-android"
    private const val REPO = "bv"
    private const val MAX_RELEASE_PAGES = 3

    /** 上次成功的加速线路（镜像前缀，空串为直连），只在进程内记忆。 */
    @Volatile
    private var preferredApiMirror: String? = null

    @Volatile
    private var preferredDownloadMirror: String? = null
    private lateinit var client: HttpClient
    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val isDebug get() = BuildConfig.DEBUG
    private val isAlpha get() = Prefs.updateAlpha

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            engine {
                // Android 8 以下的系统信任库没有 ISRG 根证书，更新线路会 TLS 失败
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    runCatching { LegacyRootTrust.open(BVApp.context) }.getOrNull()?.let { trust ->
                        config {
                            sslSocketFactory(trust.socketFactory, trust.trustManager)
                        }
                    }
                }
            }
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
        }
    }

    private suspend fun getReleases(
        owner: String = OWNER,
        repo: String = REPO,
        pageSize: Int = 30,
        page: Int = 1
    ): List<Release> = requestWithMirrors(
        url = "$API_BASE/repos/$owner/$repo/releases",
        preferredMirror = preferredApiMirror,
        onWorkingMirror = { preferredApiMirror = it },
    ) { url ->
        decode(
            client.get(url) {
                parameter("per_page", pageSize)
                parameter("page", page)
                // gh-proxy 类镜像有 CDN 缓存，加分钟级时间戳防止返回过期的列表
                parameter("t", System.currentTimeMillis() / 60_000)
            }
        )
    }

    private suspend fun getLatestRelease(
        owner: String = OWNER,
        repo: String = REPO
    ): Release = requestWithMirrors(
        url = "$API_BASE/repos/$owner/$repo/releases/latest",
        preferredMirror = preferredApiMirror,
        onWorkingMirror = { preferredApiMirror = it },
    ) { url ->
        decode(client.get(url))
    }

    suspend fun getLatestPreReleaseBuild(): Release {
        // 发布的 Alpha 构建都在前几页，没必要把历史上百个 release 全翻一遍。
        // 加速镜像可能缓存旧列表，不能依赖接口顺序取 first()，
        // 改为翻页收集后按 versionCode 取数值最大的 prerelease。
        var best: Release? = null
        for (page in 1..MAX_RELEASE_PAGES) {
            val releases = getReleases(page = page)
            if (releases.isEmpty()) break
            for (release in releases) {
                if (!release.isPreRelease) continue
                if ((release.revision ?: Int.MIN_VALUE) >= (best?.revision ?: Int.MIN_VALUE)) {
                    best = release
                }
            }
        }
        return best ?: throw GithubApiException("没有找到 Alpha 构建")
    }

    suspend fun getLatestReleaseBuild(): Release = getLatestRelease()

    suspend fun getLatestBuild(): Release =
        if (isAlpha) getLatestPreReleaseBuild() else getLatestReleaseBuild()

    /**
     * 直连失败时依次换乘加速镜像；上次成功的线路优先。
     * 404/401 是 GitHub 的确定答复，换线路也没用，直接抛出。
     */
    private suspend fun <T> requestWithMirrors(
        url: String,
        preferredMirror: String?,
        onWorkingMirror: (String) -> Unit,
        block: suspend (String) -> T,
    ): T {
        var lastFailure: Exception? = null
        var rateLimitFailure: Exception? = null
        for (candidate in GithubRoutes.candidates(url, preferredMirror)) {
            try {
                val result = block(candidate.url)
                onWorkingMirror(candidate.mirror)
                return result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                lastFailure = failure
                if (failure.isRateLimited()) rateLimitFailure = failure
                if (!failure.isRouteRetryable()) throw failure
            }
        }
        throw rateLimitFailure ?: lastFailure ?: GithubApiException("GitHub 更新线路全部不可用")
    }

    private fun Exception.isRateLimited(): Boolean =
        this is GithubApiException && (statusCode == 403 || statusCode == 429)

    private fun Exception.isRouteRetryable(): Boolean = when (this) {
        is CancellationException -> false
        is GithubApiException -> statusCode == null || statusCode == 403 || statusCode == 429 || statusCode >= 500
        else -> true
    }

    /**
     * 读取响应体；非 2xx 时抛带原因的异常，限流单独给出恢复时间。
     * 旧实现把 403 限流也当成普通失败，界面上只有一句「检查更新失败」。
     */
    private suspend inline fun <reified T> decode(response: HttpResponse): T {
        val body = response.textOrThrow()
        return try {
            json.decodeFromString<T>(body)
        } catch (e: SerializationException) {
            throw GithubApiException("解析 GitHub 返回数据失败：${e.message}", e)
        }
    }

    private suspend fun HttpResponse.textOrThrow(): String {
        val body = bodyAsText()
        if (status.isSuccess()) return body
        val apiMessage = runCatching {
            (json.parseToJsonElement(body) as? JsonObject)?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        val rateLimited = status == HttpStatusCode.Forbidden && headers["X-RateLimit-Remaining"] == "0"
        val resetTime = headers["X-RateLimit-Reset"]?.toLongOrNull()
            ?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it * 1000)) }
        throw GithubApiException(
            when {
                rateLimited && resetTime != null ->
                    "GitHub API 请求次数已用尽（每个 IP 每小时 60 次），请于 $resetTime 后重试"
                rateLimited -> "GitHub API 请求次数已用尽，请稍后重试"
                apiMessage != null -> "HTTP ${status.value}：$apiMessage"
                else -> "HTTP ${status.value} ${status.description}"
            },
            statusCode = status.value,
        )
    }

    suspend fun downloadUpdate(
        release: Release,
        file: File,
        downloadListener: ProgressListener
    ) {
        val downloadAsset = release.downloadAsset(isDebug)
            ?: throw GithubApiException("发布中没有可用的安装包：${release.assets.joinToString { it.name }}")
        try {
            requestWithMirrors(
                url = downloadAsset.browserDownloadUrl,
                preferredMirror = preferredDownloadMirror,
                onWorkingMirror = { preferredDownloadMirror = it },
            ) { url ->
                file.delete()
                client.prepareRequest {
                    url(url)
                    onDownload(downloadListener)
                }.execute { response ->
                    // Ktor 默认不因 4xx/5xx 抛异常，不校验的话错误页会被原样写成 apk
                    if (!response.status.isSuccess()) {
                        throw GithubApiException(
                            message = "下载失败：HTTP ${response.status.value}",
                            statusCode = response.status.value,
                        )
                    }
                    response.bodyAsChannel().copyAndClose(file.writeChannel())
                }
                check(file.startsWithZipMagic()) { "下载内容不是有效的 APK（可能被网络拦截）" }
                downloadAsset.sha256?.let { expected ->
                    check(file.sha256Hex().equals(expected, ignoreCase = true)) {
                        "下载内容校验失败（SHA-256 不匹配）"
                    }
                }
            }
        } catch (t: Exception) {
            file.delete()
            throw t
        }
    }
}

/** APK 就是 zip，开头必须是 "PK"；代理返回的错误页也会被存成 .apk。 */
private fun File.startsWithZipMagic(): Boolean = runCatching {
    inputStream().use { stream ->
        val header = ByteArray(2)
        stream.read(header) == 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
    }
}.getOrDefault(false)

private fun File.sha256Hex(): String? = runCatching {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }
    digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}.getOrNull()
