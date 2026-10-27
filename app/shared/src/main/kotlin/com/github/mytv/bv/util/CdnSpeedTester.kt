package com.github.mytv.bv.util

import com.github.mytv.bv.BVApp
import com.github.mytv.bv.player.PlayerHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 进程级 CDN 测速器。
 *
 * 对候选播放地址所在的 host 发一个小流量 Range 请求，用「实际读取字节数 / 耗时」换算吞吐，
 * 结果按 host 缓存约 10 分钟，避免同一 host 反复测速。测速失败（超时、非 2xx、读取异常）记 0 分，
 * 同样缓存 10 分钟，防止坏地址在短时间内被反复探测。
 *
 * 特点：
 * - 复用播放器的进程级 [PlayerHttpClient]，不新建连接池；UA/Referer 由调用方传入，
 *   与 [com.github.mytv.bv.player.impl.exo.ExoMediaPlayer] 的 OkHttpDataSource 设置保持一致；
 * - 同一 host 只探测一次，探测并发上限 [MAX_CONCURRENT_PROBES]；
 * - 只在 [rank] 内部切到 IO 线程，调用方不会阻塞主线程。
 */
object CdnSpeedTester {
    private val logger = KotlinLogging.logger("CdnSpeedTester")

    /** 测速读取的样本字节数：128 KiB */
    private const val PROBE_BYTES = 131_072L

    /** Range 请求头（末字节含 131072） */
    private const val PROBE_RANGE = "bytes=0-131072"

    /** 测速结果缓存有效期：10 分钟 */
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    /** 单个地址的探测超时 */
    private const val PROBE_TIMEOUT_MS = 2_000L

    /** 并发探测上限 */
    private const val MAX_CONCURRENT_PROBES = 3

    /** 缓存 host 数量上限，超出按最久未测淘汰 */
    private const val MAX_CACHE_ENTRIES = 128

    private data class HostScore(
        val bytesPerSecond: Long,
        val measuredAtMs: Long
    )

    private val hostScores = ConcurrentHashMap<String, HostScore>()

    /** 复用播放器的进程级 client（共享连接池），只派生短超时 */
    private val client: OkHttpClient by lazy {
        PlayerHttpClient.get(BVApp.context)
            .newBuilder()
            .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * 按实测吞吐对候选地址排序（高到低）。
     *
     * - 入参顺序即回退顺序：测速全部失败/超时、或地址没有可测的 host 时，保持入参顺序；
     * - 已测出正速度的 host 排在前面，其余（未测、失败、缓存 0 分）保持入参相对顺序排在后面；
     * - 空地址会被过滤并去重，候选 ≤ 1 时不发任何请求。
     *
     * @param userAgent 与播放请求一致的 UA；为 null 时不设置该请求头
     * @param referer 与播放请求一致的 Referer；为 null 时不设置该请求头
     */
    suspend fun rank(
        urls: List<String>,
        userAgent: String? = null,
        referer: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        val candidates = urls.filter { it.isNotBlank() }.distinct()
        if (candidates.size <= 1) return@withContext candidates

        val now = System.currentTimeMillis()
        val staleHosts = candidates
            .map(::hostOf)
            .distinct()
            .filterNot { isFresh(it, now) }
        if (staleHosts.isNotEmpty()) {
            probeHosts(staleHosts, candidates, userAgent, referer)
        }
        prune(now)

        // 稳定排序：只有测出正速度的 host 前移，其余保持调用方给的顺序（官方 CDN 优先）
        val measured = candidates.filter { (scoreOf(it) ?: 0L) > 0L }
        val others = candidates.filterNot { measured.contains(it) }
        (measured.sortedByDescending { scoreOf(it) ?: 0L } + others).also { ranked ->
            if (staleHosts.isNotEmpty()) {
                val scores = ranked.joinToString { url -> "${hostOf(url)}=${scoreOf(url) ?: -1}" }
                logger.fInfo { "CDN speed rank: $scores" }
            }
        }
    }

    private suspend fun probeHosts(
        hosts: List<String>,
        candidates: List<String>,
        userAgent: String?,
        referer: String?
    ) {
        val semaphore = Semaphore(MAX_CONCURRENT_PROBES)
        coroutineScope {
            hosts.forEach { host ->
                launch {
                    semaphore.withPermit {
                        // 同一 host 任取一个 URL 即可，测的是 host 的下载吞吐
                        val url = candidates.first { hostOf(it) == host }
                        val score = probe(url, userAgent, referer)
                        hostScores[host] = HostScore(score, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    /** 对单个地址做 Range 测速，返回吞吐（字节/秒）；失败返回 0 */
    private fun probe(url: String, userAgent: String?, referer: String?): Long {
        val startedAtNs = System.nanoTime()
        return try {
            val builder = Request
                .Builder()
                .url(url)
                .header("Range", PROBE_RANGE)
            userAgent?.let { builder.header("User-Agent", it) }
            referer?.let { builder.header("Referer", it) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.fInfo { "CDN probe non-2xx: host=${hostOf(url)} code=${response.code}" }
                    return 0L
                }
                val body = response.body ?: return 0L
                val source = body.source()
                val buffer = Buffer()
                var total = 0L
                while (total < PROBE_BYTES) {
                    val read = source.read(buffer, PROBE_BYTES - total)
                    if (read == -1L) break
                    total += read
                    buffer.clear()
                }
                val elapsedNs = System.nanoTime() - startedAtNs
                if (total <= 0L || elapsedNs <= 0L) 0L else total * 1_000_000_000L / elapsedNs
            }
        } catch (e: Exception) {
            logger.fInfo { "CDN probe failed: host=${hostOf(url)} ${e.message}" }
            0L
        }
    }

    private fun isFresh(host: String, nowMs: Long): Boolean =
        hostScores[host]?.let { nowMs - it.measuredAtMs <= CACHE_TTL_MS } == true

    private fun scoreOf(url: String): Long? = hostScores[hostOf(url)]?.bytesPerSecond

    private fun hostOf(url: String): String =
        runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: url

    /** 丢弃过期条目；超过上限时淘汰最久未测的 */
    private fun prune(nowMs: Long) {
        if (hostScores.size <= MAX_CACHE_ENTRIES) return
        hostScores.entries.removeAll { nowMs - it.value.measuredAtMs > CACHE_TTL_MS }
        if (hostScores.size <= MAX_CACHE_ENTRIES) return
        hostScores.entries
            .sortedByDescending { it.value.measuredAtMs }
            .drop(MAX_CACHE_ENTRIES)
            .forEach { hostScores.remove(it.key) }
    }
}
