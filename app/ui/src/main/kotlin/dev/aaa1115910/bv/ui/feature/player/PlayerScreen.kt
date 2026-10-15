package dev.aaa1115910.bv.ui.feature.player

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.navigation.PlayerRoute
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * 全屏播放页（PlayerRoute）：电视详情页的「播放」与其他入口都走这里；手机 / 平板上同样是全屏播放器。
 * 照抄旧 VideoPlayerV3Activity：创建播放内核、读取参数后 loadPlayUrl；离开时内核由 BvVideoPlayer 释放。
 */
@Composable
fun PlayerScreen(nav: NavController, route: PlayerRoute) {
    val playerVm: VideoPlayerV3ViewModel = koinViewModel()
    val commentVm: CommentViewModel = koinViewModel()
    val videoDetailRepository: VideoDetailRepository = koinInject()
    val profile = LocalDeviceProfile.current
    val isTv = profile.isTv
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    val videoPlayer = remember { createVideoPlayer(context) }
    // 重新进入时 ViewModel 里可能还是已被释放的弹幕播放器，等 loadPlayUrl 创建新的之后再交给播放器
    val staleDanmaku = remember { playerVm.danmakuPlayer }
    val launchExtras = remember(route.aid) { PlayerLaunchExtras.get(route.aid) }

    var showRemoteHint by remember { mutableStateOf(isTv && !Prefs.showedRemoteControllerPanelDemo) }
    var showComments by rememberSaveable { mutableStateOf(false) }
    val playerFocus = remember { FocusRequester() }

    val exit: () -> Unit = {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) nav.popBackStack()
    }

    DisposableEffect(videoPlayer) {
        playerVm.videoPlayer = videoPlayer
        onDispose {
            if (playerVm.videoPlayer === videoPlayer) playerVm.videoPlayer = null
            // release() 可重复调用；播放内核由 BvVideoPlayer 释放
            playerVm.danmakuPlayer?.release()
        }
    }

    // 读取参数并开始播放（旧 VideoPlayerV3Activity.getParamsFromIntent）
    LaunchedEffect(videoPlayer) {
        val request = PlayRequest(
            aid = route.aid,
            cid = route.cid,
            title = route.title,
            partTitle = route.partTitle,
            epid = route.epid.takeIf { it != 0 },
            seasonId = route.seasonId.takeIf { it != 0 },
            fromSeason = route.fromSeason,
            subType = route.subType,
            startMs = route.played.toLong(),
            playerIconIdle = route.playerIconIdle,
            playerIconMoving = route.playerIconMoving,
        )
        playerVm.isVerticalVideo = route.isVerticalVideo
        playerVm.proxyArea = ProxyArea.entries.getOrElse(route.proxyArea) { ProxyArea.MainLand }
        commentVm.commentType = 1L
        commentVm.commentId = route.aid
        // 沿用旧版：played 作为「回到上次进度」提示
        startPlayback(
            playerVm = playerVm,
            videoPlayer = videoPlayer,
            request = request,
            videoDetailRepository = videoDetailRepository,
            useHistoryTip = true,
        )
    }

    // 手机 / 平板：横屏（竖屏视频保持竖屏）+ 隐藏系统栏；离开时恢复
    if (!isTv) {
        val activity = remember(context) { context.findActivity() }
        DisposableEffect(activity, route.isVerticalVideo) {
            val window = activity?.window
            val original = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val isPhoneDevice = configuration.smallestScreenWidthDp < 600
            if (activity != null && isPhoneDevice && !route.isVerticalVideo) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                if (activity != null && isPhoneDevice && !route.isVerticalVideo) {
                    activity.requestedOrientation = original
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .focusRequester(playerFocus)
        ) {
            BvPlayerHost(
                playerVm = playerVm,
                videoPlayer = videoPlayer,
                danmakuPlayer = playerVm.danmakuPlayer.takeIf { it !== staleDanmaku },
                cover = launchExtras?.cover,
                isTv = isTv,
                isTablet = profile.layout == DeviceLayout.Tablet,
                onExit = exit,
                modifier = Modifier.fillMaxSize(),
                isFullScreen = true,
                onExitFullScreen = exit,
                ownerName = launchExtras?.ownerName.orEmpty(),
                onOpenComments = if (isTv) {
                    {
                        showComments = true
                        if (commentVm.comments.isEmpty() && commentVm.commentId != 0L) {
                            scope.launch { commentVm.refreshComments() }
                        }
                    }
                } else null,
                showRemoteControlHint = showRemoteHint,
                onRemoteControlHintDismiss = {
                    Prefs.showedRemoteControllerPanelDemo = true
                    showRemoteHint = false
                },
            )
        }
        if (isTv && showComments) {
            TvCommentsPanel(
                commentVm = commentVm,
                onDismiss = {
                    showComments = false
                    scope.launch {
                        // 等面板离开组合后焦点回到播放器
                        kotlinx.coroutines.delay(16)
                        runCatching { playerFocus.requestFocus() }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * 进入 PlayerRoute 前由详情页登记的附加信息（路由参数里没有）：封面（播放器的封面取色）与 UP 主名字
 * （电视左上角副标题「{视频标题} · {UP}」）。
 */
internal object PlayerLaunchExtras {
    data class Extras(val cover: String?, val ownerName: String)

    private val extras = HashMap<Long, Extras>()

    fun put(aid: Long, cover: String?, ownerName: String) {
        synchronized(extras) {
            if (extras.size > 32) extras.clear()
            extras[aid] = Extras(cover, ownerName)
        }
    }

    fun get(aid: Long): Extras? = synchronized(extras) { extras[aid] }
}
