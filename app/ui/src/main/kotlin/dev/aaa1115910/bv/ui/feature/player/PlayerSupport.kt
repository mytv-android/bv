package dev.aaa1115910.bv.ui.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.snapshotFlow
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import dev.aaa1115910.bv.R as BvR

private val logger = KotlinLogging.logger("PlayerSupport")

/** 创建播放内核（照抄旧 VideoPlayerV3Activity / 手机 VideoPlayerActivity 的 initVideoPlayer）。 */
internal fun createVideoPlayer(context: Context): AbstractVideoPlayer {
    val apiType = Prefs.apiType
    val options = VideoPlayerOptions(
        userAgent = when (apiType) {
            ApiType.Web -> context.getString(BvR.string.video_player_user_agent_http)
            ApiType.App -> context.getString(BvR.string.video_player_user_agent_client)
        },
        referer = when (apiType) {
            ApiType.Web -> context.getString(BvR.string.video_player_referer)
            ApiType.App -> null
        },
        enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
        enableSkipSilence = Prefs.playerSkipSilence,
        audioBalanceLevel = Prefs.playerAudioBalanceLevel
    )
    return when (Prefs.playerType) {
        PlayerType.Media3 -> ExoPlayerFactory().create(context, options)
    }
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * 一次播放请求（PlayerRoute 的参数，或详情页内嵌播放器要播放的分 P / 剧集）。
 *
 * @param startMs 开始播放的位置（毫秒），0 表示从头
 */
internal data class PlayRequest(
    val aid: Long,
    val cid: Long,
    val title: String,
    val partTitle: String,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val fromSeason: Boolean = false,
    val subType: Int = 0,
    val startMs: Long = 0L,
    val playerIconIdle: String = "",
    val playerIconMoving: String = "",
)

/**
 * cid 未知（0）时补全 aid / cid：番剧用 epid 查（旧手机 VideoPlayerActivity），
 * 普通视频取详情的第一个分 P（旧电视 SeasonInfo 跳回 VideoInfo 取 cid 的做法）。
 */
internal suspend fun resolveAidCid(
    aid: Long,
    cid: Long,
    epid: Int?,
    videoDetailRepository: VideoDetailRepository,
): Pair<Long, Long> {
    if (cid != 0L) return aid to cid
    return withContext(Dispatchers.IO) {
        val byEpid = epid?.takeIf { it != 0 }?.let { BiliHttpApi.getAidCidByEpid(it) }
        if (byEpid != null) return@withContext byEpid
        runCatching {
            val detail = videoDetailRepository.getVideoDetail(aid = aid, preferApiType = Prefs.apiType)
            detail.aid to detail.cid
        }.onFailure {
            logger.warn { "Resolve cid of av$aid failed: ${it.stackTraceToString()}" }
        }.getOrDefault(aid to cid)
    }
}

/**
 * 让 [VideoPlayerV3ViewModel] 播放 [request]（照抄旧 VideoPlayerV3Activity.getParamsFromIntent），
 * [resumeMs] > 0 时在新媒体准备后跳到该位置（同步弹幕）。必须在主线程调用。
 */
internal suspend fun startPlayback(
    playerVm: VideoPlayerV3ViewModel,
    videoPlayer: AbstractVideoPlayer,
    request: PlayRequest,
    videoDetailRepository: VideoDetailRepository,
    resumeMs: Long = request.startMs,
    useHistoryTip: Boolean = false,
) {
    val (aid, cid) = resolveAidCid(request.aid, request.cid, request.epid, videoDetailRepository)
    logger.info { "Start playback: [aid=$aid, cid=$cid, epid=${request.epid}, resume=$resumeMs]" }
    playerVm.title = request.title
    playerVm.partTitle = request.partTitle
    playerVm.fromSeason = request.fromSeason
    playerVm.subType = request.subType
    playerVm.playerIconIdle = request.playerIconIdle
    playerVm.playerIconMoving = request.playerIconMoving
    // 电视沿用旧版「回到上次进度」提示；触摸端内嵌播放器直接跳到上次进度
    playerVm.lastPlayed = if (useHistoryTip) resumeMs.toInt() else 0
    playerVm.showBuffering = false
    playerVm.loadPlayUrl(
        avid = aid,
        cid = cid,
        epid = request.epid?.takeIf { it != 0 },
        seasonId = request.seasonId?.takeIf { it != 0 },
    )
    if (useHistoryTip || resumeMs <= 0L) return
    // ViewModel 在主线程 prepare() 之后立即把 showBuffering 置为 true，此时跳转对新的媒体生效
    val prepared = withTimeoutOrNull(20_000L) {
        snapshotFlow { playerVm.showBuffering }.first { it }
    }
    if (prepared == null || playerVm.videoPlayer !== videoPlayer) return
    videoPlayer.seekTo(resumeMs)
    runCatching {
        playerVm.danmakuView?.notifySeek(resumeMs)
    }
}
