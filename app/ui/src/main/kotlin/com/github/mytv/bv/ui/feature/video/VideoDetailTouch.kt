package com.github.mytv.bv.ui.feature.video

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.OrientationEventListener
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import com.github.mytv.bv.player.danmaku.DanmakuView
import com.github.mytv.bv.player.entity.VideoListItemData
import com.github.mytv.bv.player.entity.VideoListInteractiveNode
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.feature.player.BvPlayerHost
import com.github.mytv.bv.ui.feature.player.PlayRequest
import com.github.mytv.bv.ui.feature.player.createVideoPlayer
import com.github.mytv.bv.ui.feature.player.findActivity
import com.github.mytv.bv.ui.feature.player.startPlayback
import com.github.mytv.bv.viewmodel.VideoPlayerV3ViewModel
import kotlinx.coroutines.delay

/** 内嵌播放器参数（随重组更新）。 */
@Immutable
internal data class EmbeddedPlayerArgs(
    val cover: String?,
    val ownerName: String,
    val isTablet: Boolean,
)

/**
 * 手机 / 平板详情页的外壳（第 9.3 节行为）：
 * - 内嵌播放器只有一个实例（movableContentOf），进入全屏（按钮，或手机横过来）时原地切换为全屏控制层，
 *   播放不中断；返回键 / 竖回来退出全屏；平板 ↔ 手机布局切换时播放器同样原地移动；
 * - 手机全屏横屏 + 隐藏系统栏；退出全屏后先锁竖屏，等手机真正竖回来再恢复自动旋转；
 * - 手机布局顶部是黑色状态栏区域，状态栏图标改为浅色。
 */
@Composable
internal fun TouchDetailHost(
    layout: DeviceLayout,
    holder: DetailPlaybackHolder,
    playerVm: VideoPlayerV3ViewModel,
    args: EmbeddedPlayerArgs,
    videoDetailRepository: VideoDetailRepository,
    onBeforePlay: (PlayRequest) -> Unit,
    onPlayerLoadNewVideo: (VideoListItemData) -> Unit,
    onExit: () -> Unit,
    onEmbeddedMore: () -> Unit,
    phone: @Composable (playerSlot: @Composable (Modifier) -> Unit) -> Unit,
    tablet: @Composable (playerSlot: @Composable (Modifier) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isPhoneDevice = configuration.smallestScreenWidthDp < 600
    val originalOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    var lockedPortrait by remember { mutableStateOf(false) }

    val isLandscapeNow: () -> Boolean = {
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val enterFullscreen: () -> Unit = {
        holder.fullscreen = true
        val width = playerVm.currentVideoWidth
        val height = playerVm.currentVideoHeight
        val vertical = width > 0 && height > width
        if (isPhoneDevice && !vertical && activity != null && !isLandscapeNow()) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    val exitFullscreen: () -> Unit = {
        holder.fullscreen = false
        if (isPhoneDevice && activity != null) {
            if (isLandscapeNow()) {
                // 手机还横着：先锁竖屏，等手机真正竖回来再恢复（否则会立刻又自动进入全屏）
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                lockedPortrait = true
            } else {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    // 手机横过来自动全屏，竖回去退出
    LaunchedEffect(landscape, isPhoneDevice) {
        if (!isPhoneDevice) return@LaunchedEffect
        if (landscape) {
            if (!holder.fullscreen && !lockedPortrait) holder.fullscreen = true
        } else if (holder.fullscreen &&
            activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        ) {
            holder.fullscreen = false
        }
    }

    // 退出全屏后锁定竖屏：手机真正竖回来后恢复原来的方向设置
    DisposableEffect(lockedPortrait, activity) {
        if (!lockedPortrait || activity == null) return@DisposableEffect onDispose { }
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return
                if (orientation <= 20 || orientation >= 340) {
                    view.post {
                        if (lockedPortrait) {
                            activity.requestedOrientation = originalOrientation
                            lockedPortrait = false
                        }
                    }
                }
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        } else {
            activity.requestedOrientation = originalOrientation
            lockedPortrait = false
        }
        onDispose { listener.disable() }
    }

    // 全屏时隐藏系统栏
    val fullscreen = holder.fullscreen
    DisposableEffect(fullscreen, activity) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (fullscreen) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (fullscreen) controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // 手机布局：视频上方是黑色状态栏区域，状态栏图标用浅色
    val phoneLayout = layout != DeviceLayout.Tablet
    DisposableEffect(phoneLayout, activity) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        if (phoneLayout) controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (before != null) controller?.isAppearanceLightStatusBars = before
        }
    }

    // 离开详情页：退出全屏并恢复屏幕方向
    DisposableEffect(activity) {
        onDispose {
            holder.fullscreen = false
            activity?.requestedOrientation = originalOrientation
        }
    }

    BackHandler(enabled = fullscreen) { exitFullscreen() }

    val latestArgs by rememberUpdatedState(args)
    val latestEnter by rememberUpdatedState(enterFullscreen)
    val latestExit by rememberUpdatedState(exitFullscreen)
    val latestOnExit by rememberUpdatedState(onExit)
    val latestMore by rememberUpdatedState(onEmbeddedMore)
    val latestBeforePlay by rememberUpdatedState(onBeforePlay)
    val latestLoadNew by rememberUpdatedState(onPlayerLoadNewVideo)

    // 唯一的播放器实例：在内嵌位置和全屏层之间移动，不会重建
    val player = remember(playerVm, holder) {
        movableContentOf { modifier: Modifier, isFullScreen: Boolean ->
            DetailEmbeddedPlayer(
                modifier = modifier,
                isFullScreen = isFullScreen,
                playerVm = playerVm,
                holder = holder,
                args = latestArgs,
                videoDetailRepository = videoDetailRepository,
                onEnterFullScreen = { latestEnter() },
                onExitFullScreen = { latestExit() },
                onExit = { latestOnExit() },
                onEmbeddedMore = { latestMore() },
                onBeforePlay = { latestBeforePlay(it) },
                onLoadNewVideo = { latestLoadNew(it) },
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        val slot: @Composable (Modifier) -> Unit = { slotModifier ->
            if (holder.fullscreen) {
                Box(slotModifier.background(Color.Black))
            } else {
                player(slotModifier, false)
            }
        }
        if (layout == DeviceLayout.Tablet) tablet(slot) else phone(slot)
        if (holder.fullscreen) {
            player(Modifier.fillMaxSize(), true)
        }
    }
}

/**
 * 详情页内嵌播放器：照抄旧手机 VideoPlayerActivity 创建播放内核（每次进入组合创建一个，离开时由
 * BvVideoPlayer 释放），按 [DetailPlaybackHolder.request] 播放；被重建时从上次的分 P 与进度继续。
 */
@Composable
private fun DetailEmbeddedPlayer(
    modifier: Modifier,
    isFullScreen: Boolean,
    playerVm: VideoPlayerV3ViewModel,
    holder: DetailPlaybackHolder,
    args: EmbeddedPlayerArgs,
    videoDetailRepository: VideoDetailRepository,
    onEnterFullScreen: () -> Unit,
    onExitFullScreen: () -> Unit,
    onExit: () -> Unit,
    onEmbeddedMore: () -> Unit,
    onBeforePlay: (PlayRequest) -> Unit,
    onLoadNewVideo: (VideoListItemData) -> Unit,
) {
    val context = LocalContext.current
    val videoPlayer = remember { createVideoPlayer(context) }
    // 自研弹幕引擎 View：每个播放页一个实例，交给 ViewModel 喂数据，离开页面时释放
    val danmakuView = remember { DanmakuView(context).also { playerVm.danmakuView = it } }
    val latestBeforePlay by rememberUpdatedState(onBeforePlay)

    DisposableEffect(videoPlayer) {
        playerVm.videoPlayer = videoPlayer
        onDispose {
            if (playerVm.videoPlayer === videoPlayer) playerVm.videoPlayer = null
        }
    }

    DisposableEffect(danmakuView) {
        onDispose {
            // release() 幂等（onDetachedFromWindow 也会触发一次）
            danmakuView.release()
            if (playerVm.danmakuView === danmakuView) playerVm.danmakuView = null
        }
    }

    LaunchedEffect(videoPlayer, holder.requestId) {
        val request = holder.request ?: return@LaunchedEffect
        latestBeforePlay(request)
        // 页面被重建（离开后返回、配置变化）时，从上次播放的分 P 与进度继续
        val lastCid = holder.lastCid
        val resumed = if (lastCid != 0L && lastCid != request.cid) {
            val item = playerVm.availableVideoList
                .filterIsInstance<VideoListItemData>()
                .firstOrNull { it.cid == lastCid }
            if (item != null) {
                request.copy(
                    aid = item.aid,
                    cid = item.cid,
                    epid = item.epid ?: request.epid,
                    partTitle = (item as? VideoListInteractiveNode)?.partTitle?.takeIf { it.isNotBlank() }
                        ?: item.title,
                    startMs = (item as? VideoListInteractiveNode)?.startPos?.times(1000L) ?: 0L,
                )
            } else request
        } else request
        startPlayback(
            playerVm = playerVm,
            videoPlayer = videoPlayer,
            request = resumed,
            videoDetailRepository = videoDetailRepository,
            resumeMs = holder.lastPositionMs,
        )
    }

    // 记录进度（媒体准备好之后），重建时从这里继续
    LaunchedEffect(videoPlayer) {
        while (true) {
            delay(1000)
            if (playerVm.videoPlayer === videoPlayer && playerVm.showBuffering &&
                videoPlayer.duration > 0 && playerVm.currentCid != 0L
            ) {
                holder.lastPositionMs = videoPlayer.currentPosition
                holder.lastCid = playerVm.currentCid
            }
        }
    }

    BvPlayerHost(
        playerVm = playerVm,
        videoPlayer = videoPlayer,
        danmakuView = danmakuView,
        cover = args.cover,
        isTv = false,
        isTablet = args.isTablet,
        onExit = onExit,
        modifier = modifier,
        isFullScreen = isFullScreen,
        onEnterFullScreen = onEnterFullScreen,
        onExitFullScreen = onExitFullScreen,
        ownerName = args.ownerName,
        onEmbeddedMore = onEmbeddedMore,
        onLoadNewVideo = onLoadNewVideo,
    )
}
