package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.util.CdnSpeedTester
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

/**
 * 起播 CDN 选源与播放器自动切候选的接入点（小而独立，后续批次在 ViewModel 中调用即可）。
 *
 * 当前 [dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel] 仍走旧的两行：
 * ```
 * videoPlayer.playUrl(videoUrl, audioUrl)
 * videoPlayer.prepare()
 * ```
 * 接入时替换为：
 * ```
 * playbackCdnController.play(
 *     player = videoPlayer,
 *     videoUrls = streams.videoUrls,
 *     audioUrls = streams.audioUrls,
 *     userAgent = options.userAgent,
 *     referer = options.referer,
 * )
 * ```
 * 或只取排序结果自行起播：
 * ```
 * val candidates = playbackCdnController.order(streams.videoUrls, streams.audioUrls, ua, referer)
 * videoPlayer.playUrl(candidates.videoUrls, candidates.audioUrls)
 * videoPlayer.prepare()
 * ```
 *
 * 行为：
 * - [Prefs.autoSelectCdn]（默认关闭）打开且候选 > 1 时，用 [CdnSpeedTester] 测速后排序；
 *   关闭、候选不足或测速全部失败时保持 [dev.aaa1115910.bv.repository.PlayUrlResolver] 给出的顺序
 *   （官方 CDN 优先）；
 * - 无论是否测速，都会把完整候选列表交给播放器；[Prefs.enableCdnFailover]（默认开启）控制
 *   播放失败/频繁缓冲时是否自动切下一个地址（切换逻辑在播放器内部，见 ExoMediaPlayer）。
 */
@Single
class PlaybackCdnController {
    companion object {
        private val logger = KotlinLogging.logger("PlaybackCdnController")
    }

    /** 起播候选地址（首个为本次实际起播地址） */
    data class Candidates(
        val videoUrls: List<String>,
        val audioUrls: List<String>
    ) {
        val videoUrl: String? get() = videoUrls.firstOrNull()
        val audioUrl: String? get() = audioUrls.firstOrNull()
    }

    /**
     * 按设置对候选地址排序。挂起函数，内部切 IO，不阻塞主线程。
     *
     * @param userAgent 与播放请求一致的 UA；[userAgent] 与 [referer] 都为 null 时按当前
     *   [Prefs.apiType] 取与播放器相同的默认值
     * @param referer 与播放请求一致的 Referer
     */
    suspend fun order(
        videoUrls: List<String>,
        audioUrls: List<String>,
        userAgent: String? = null,
        referer: String? = null
    ): Candidates = withContext(Dispatchers.IO) {
        val videoCandidates = videoUrls.filter { it.isNotBlank() }.distinct()
        val audioCandidates = audioUrls.filter { it.isNotBlank() }.distinct()
        if (!Prefs.autoSelectCdn || (videoCandidates.size <= 1 && audioCandidates.size <= 1)) {
            return@withContext Candidates(videoCandidates, audioCandidates)
        }

        val (ua, ref) = resolveHeaders(userAgent, referer)
        logger.fInfo {
            "Auto select CDN: video=${videoCandidates.size}, audio=${audioCandidates.size}, apiType=${Prefs.apiType}"
        }
        Candidates(
            videoUrls = CdnSpeedTester.rank(videoCandidates, ua, ref),
            audioUrls = CdnSpeedTester.rank(audioCandidates, ua, ref)
        )
    }

    /**
     * 起播：按设置测速排序后把候选列表交给播放器并 prepare（内部切主线程）。
     * 调用方无需再调用 `playUrl` / `prepare`。
     */
    suspend fun play(
        player: AbstractVideoPlayer,
        videoUrls: List<String>,
        audioUrls: List<String>,
        userAgent: String? = null,
        referer: String? = null
    ) {
        val candidates = order(videoUrls, audioUrls, userAgent, referer)
        val failoverEnabled = withContext(Dispatchers.IO) { Prefs.enableCdnFailover }
        withContext(Dispatchers.Main) {
            player.setCdnFailoverEnabled(failoverEnabled)
            player.playUrl(candidates.videoUrls, candidates.audioUrls)
            player.prepare()
        }
    }

    /** 未显式传入请求头时，回退到与 ExoMediaPlayer 播放请求一致的 UA / Referer */
    private fun resolveHeaders(userAgent: String?, referer: String?): Pair<String?, String?> {
        if (userAgent != null || referer != null) return userAgent to referer
        val context = BVApp.context
        return when (Prefs.apiType) {
            ApiType.Web -> context.getString(R.string.video_player_user_agent_http) to
                context.getString(R.string.video_player_referer)

            ApiType.App -> context.getString(R.string.video_player_user_agent_client) to null
        }
    }
}
