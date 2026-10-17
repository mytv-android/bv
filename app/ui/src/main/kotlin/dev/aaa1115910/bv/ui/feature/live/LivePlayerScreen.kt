@file:OptIn(ExperimentalLayoutApi::class)

package dev.aaa1115910.bv.ui.feature.live

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.entity.live.RoomRealTimeMessageUpdateEvent
import dev.aaa1115910.biliapi.http.entity.live.WatchedChangeEvent
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.player.BvDanmakuPlayer
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.danmaku.DanmakuConfig
import dev.aaa1115910.bv.player.danmaku.DanmakuView
import dev.aaa1115910.bv.player.danmaku.model.Danmaku
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.CinemaScrim
import dev.aaa1115910.bv.ui.designsystem.focus.BvKey
import dev.aaa1115910.bv.ui.designsystem.focus.toBvKey
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.me.formatCount
import dev.aaa1115910.bv.ui.feature.player.findActivity
import dev.aaa1115910.bv.ui.navigation.LivePlayerRoute
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.live.LivePlayerViewModel
import dev.aaa1115910.bv.R as BvR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

/** 控制层显示后自动隐藏的时间。 */
private const val AutoHideMillis = 4000L

/** 直播弹幕按用户等级过滤的可选档位（LV0 = 不过滤）。 */
private val LiveDanmakuFilterLevelPresets = listOf(0, 10, 20, 30, 40, 50, 60)

/**
 * 直播间播放页（LivePlayerRoute）：播放器 + 实时弹幕 + 控制层，替换 LivePlayerPlaceholder。
 *
 * - 播放：进页 [LivePlayerViewModel.load] → 当前线路 url → ExoMediaPlayer（HLS 直播，单一地址）；
 *   画质 / 线路切换、刷新都会替换 playInfo，UI 以 url 变化为准重新起播。
 * - 弹幕：VM 的 WS 事件流收集后 append 进自研弹幕引擎 DanmakuView（主线程）；进房先刷一遍历史弹幕（失败静默）。
 * - 触屏：点击画面显隐控制层（几秒自动隐藏）；电视：确认键显隐控制栏、菜单键打开菜单、左右键切线路。
 */
@Composable
fun LivePlayerScreen(nav: NavController, route: LivePlayerRoute) {
    val vm: LivePlayerViewModel = koinViewModel()
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val isTv = LocalDeviceProfile.current.isTv

    // 播放内核：直播统一 Web UA + live referer；单一地址播放（无多候选切换）
    val videoPlayer = remember {
        ExoPlayerFactory().create(
            context,
            VideoPlayerOptions(
                userAgent = context.getString(BvR.string.video_player_user_agent_http),
                referer = "https://live.bilibili.com",
                enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
            )
        )
    }
    // 弹幕引擎 View：离开页面（离开组合）时 release；位置 / 播放状态 / 倍速跟随直播流播放器
    val danmakuView = remember {
        DanmakuView(context).apply {
            setPositionProvider { videoPlayer.currentPosition.coerceAtLeast(0L) }
            setIsPlayingProvider { videoPlayer.isPlaying }
            setPlaybackSpeedProvider { videoPlayer.speed }
            setConfig(
                DanmakuConfig(
                    textSizeScale = (Prefs.defaultDanmakuScale * 100).toInt(),
                    opacity = Prefs.defaultDanmakuOpacity,
                )
            )
        }
    }
    DisposableEffect(danmakuView) {
        onDispose {
            // release() 幂等（onDetachedFromWindow 也会触发一次）
            danmakuView.release()
        }
    }

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaybackError by remember { mutableStateOf(false) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var danmakuEnabled by rememberSaveable { mutableStateOf(Prefs.defaultDanmakuEnabled) }
    var danmakuFilterLevel by rememberSaveable { mutableIntStateOf(Prefs.defaultLiveDanmakuFilterLevel) }
    var barsVisible by rememberSaveable { mutableStateOf(false) }

    // 人气 / 看过人数：初始取 roomInfo，WS 事件更新
    var watchedText by remember { mutableStateOf<String?>(null) }
    var popularity by remember { mutableIntStateOf(-1) }

    val latestDanmakuEnabled by rememberUpdatedState(danmakuEnabled)

    val onExit: () -> Unit = {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) nav.popBackStack()
    }

    val rootFocusRequester = remember { FocusRequester() }
    val buttonFocusRequester = remember { FocusRequester() }
    var buttonsFocused by remember { mutableStateOf(false) }

    // ------------------------------------------------------------- 数据加载

    LaunchedEffect(route.roomId) {
        vm.load(route.roomId)
    }

    // 房间信息到达后初始化信息栏
    LaunchedEffect(vm.roomInfo) {
        val info = vm.roomInfo ?: return@LaunchedEffect
        watchedText = info.watchedShow?.textLarge?.takeIf { it.isNotBlank() }
            ?: info.watchedShow?.textSmall?.takeIf { it.isNotBlank() }
        popularity = info.online
    }

    // 起播：playInfo 中当前线路的 url 变化（加载 / 切画质 / 切线路 / 刷新）时重新 prepare
    val streamUrl = vm.playInfo?.lines?.getOrNull(vm.currentLineIndex)?.url
    LaunchedEffect(streamUrl) {
        if (streamUrl == null) return@LaunchedEffect
        isPlaybackError = false
        isBuffering = true
        videoPlayer.playUrl(streamUrl)
        videoPlayer.prepare()
    }

    val playerListener = remember {
        object : VideoPlayerListener {
            override fun onError(error: Exception) {
                isPlaybackError = true
                isBuffering = false
            }

            override fun onReady() {
                isPlaybackError = false
                val w = videoPlayer.videoWidth
                val h = videoPlayer.videoHeight
                if (w > 0 && h > 0) {
                    val ratio = w.toFloat() / h
                    if (ratio > 0 && ratio.isFinite()) videoAspect = ratio
                }
                videoPlayer.start()
            }

            override fun onPlay() {
                isBuffering = false
                // 弹幕引擎通过 isPlayingProvider / config.enabled 自行跟随，无需显式 start
            }

            override fun onPause() = Unit

            override fun onBuffering() {
                isBuffering = true
            }

            override fun onEnd() {
                isBuffering = false
            }

            override fun onIdle() = Unit

            override fun onSeekBack(seekBackIncrementMs: Long) = Unit
            override fun onSeekForward(seekForwardIncrementMs: Long) = Unit
        }
    }

    // ------------------------------------------------------------- 弹幕

    // 弹幕关闭时直接停掉引擎；新事件直接丢弃并推进游标，重新打开不会涌出积压
    LaunchedEffect(danmakuEnabled) {
        danmakuView.setConfig(
            DanmakuConfig(
                enabled = danmakuEnabled,
                textSizeScale = (Prefs.defaultDanmakuScale * 100).toInt(),
                opacity = Prefs.defaultDanmakuOpacity,
            )
        )
    }

    // WS 实时事件：弹幕 send（主线程），人气 / 看过更新；游标去重（StateFlow 会重放）
    var consumedDanmaku by remember { mutableIntStateOf(0) }
    LaunchedEffect(vm.danmakuEvents) {
        vm.danmakuEvents.collect { events ->
            if (events.size < consumedDanmaku) consumedDanmaku = 0 // 房间重载清空
            if (!latestDanmakuEnabled) {
                consumedDanmaku = events.size
                return@collect
            }
            val fresh: List<LiveEvent> = events.drop(consumedDanmaku)
            consumedDanmaku = events.size
            fresh.forEach { event ->
                when (event) {
                    is DanmakuEvent -> {
                        // 按发送者等级过滤（等级低于设置的丢弃）
                        if (event.userLevel >= danmakuFilterLevel) {
                            sendLiveDanmaku(danmakuView, videoPlayer, event.content)
                        }
                    }
                    is WatchedChangeEvent -> watchedText = event.textLarge
                    is RoomRealTimeMessageUpdateEvent -> popularity = event.popularity
                    else -> Unit
                }
            }
        }
    }

    // 进房刷一遍历史弹幕（失败静默）
    LaunchedEffect(vm.roomInfo) {
        val info = vm.roomInfo ?: return@LaunchedEffect
        runCatching {
            withContext(Dispatchers.IO) {
                BiliLiveHttpApi.getLiveDanmuHistory(info.roomId.takeIf { it > 0 } ?: route.roomId)
                    .getResponseData()
            }
        }.onSuccess { history ->
            history.room
                .filter { it.dmType == 0 }
                .forEach { item -> sendLiveDanmaku(danmakuView, videoPlayer, item.text) }
        }
    }

    // ------------------------------------------------------------- 切换成功提示

    // 发起切换时置位，VM 成功更新状态时消费（失败由 VM toast；初始加载不提示）
    var pendingQualityToast by remember { mutableStateOf(false) }
    var pendingLineToast by remember { mutableStateOf(false) }
    LaunchedEffect(vm.currentQn) {
        if (pendingQualityToast) {
            pendingQualityToast = false
            vm.playInfo?.qualities
                ?.firstOrNull { it.first == vm.currentQn }?.second
                ?.let { context.getString(R.string.live_player_switched_quality, it).toast(context) }
        }
    }
    LaunchedEffect(vm.currentLineIndex) {
        if (pendingLineToast) {
            pendingLineToast = false
            context.getString(
                R.string.live_player_switched_line,
                context.getString(R.string.live_player_line_label, vm.currentLineIndex + 1),
            ).toast(context)
        }
    }

    // ------------------------------------------------------------- 控制层显隐

    val showControls: () -> Unit = {
        controlsVisible = true
        interactionTick++
    }
    val hideControls: () -> Unit = {
        if (isTv) rootFocusRequester.requestFocus(scope)
        controlsVisible = false
        interactionTick++
    }
    val toggleControls: () -> Unit = { if (controlsVisible) hideControls() else showControls() }
    val closeMenu: () -> Unit = {
        menuOpen = false
        interactionTick++
        if (isTv) rootFocusRequester.requestFocus(scope)
    }

    // 电视端按键（确认显隐控制栏；菜单键开菜单；左右键切线路；返回关菜单或退出）
    val onKeyDown: (BvKey) -> Boolean = handler@{ key ->
        if (menuOpen) return@handler key == BvKey.Back || key == BvKey.Menu
        if (buttonsFocused && controlsVisible) {
            return@handler when (key) {
                BvKey.Back, BvKey.Menu, BvKey.Up, BvKey.Down, BvKey.CenterLong -> true
                else -> false // 确认 / 左右交给聚焦的按钮
            }
        }
        when (key) {
            BvKey.Center, BvKey.CenterLong, BvKey.Up, BvKey.Down, BvKey.PlayPause -> true
            BvKey.Left, BvKey.Rewind -> {
                pendingLineToast = true
                vm.switchLine(vm.currentLineIndex - 1)
                true
            }

            BvKey.Right, BvKey.FastForward -> {
                pendingLineToast = true
                vm.switchLine(vm.currentLineIndex + 1)
                true
            }

            BvKey.Menu, BvKey.Back -> true
            else -> false
        }
    }
    val onKeyUp: (BvKey) -> Boolean = handler@{ key ->
        if (menuOpen) {
            if (key == BvKey.Back || key == BvKey.Menu) {
                closeMenu()
                return@handler true
            }
            return@handler false
        }
        if (buttonsFocused && controlsVisible) {
            return@handler when (key) {
                BvKey.Back, BvKey.Up -> {
                    hideControls()
                    true
                }

                BvKey.Menu -> {
                    menuOpen = true
                    interactionTick++
                    true
                }

                else -> false
            }
        }
        when (key) {
            BvKey.Center -> {
                toggleControls()
                true
            }

            BvKey.Up, BvKey.Down -> {
                showControls()
                buttonFocusRequester.requestFocus(scope)
                true
            }

            BvKey.Menu -> {
                menuOpen = true
                interactionTick++
                true
            }

            BvKey.Back -> {
                onExit()
                true
            }

            else -> false
        }
    }

    LaunchedEffect(isTv) {
        if (isTv) runCatching { rootFocusRequester.requestFocus() }
    }

    // 控制层自动隐藏（菜单打开时挂起）
    LaunchedEffect(controlsVisible, interactionTick, menuOpen) {
        if (controlsVisible && !menuOpen) {
            delay(AutoHideMillis)
            if (!menuOpen) hideControls()
        }
    }

    // 电视端控制层出现后把焦点交给按钮行
    LaunchedEffect(controlsVisible, menuOpen, isTv) {
        if (isTv && controlsVisible && !menuOpen) {
            delay(16)
            runCatching { buttonFocusRequester.requestFocus() }
        }
    }

    // ------------------------------------------------------------- 沉浸与横屏

    if (!isTv) {
        val activity = remember(context) { context.findActivity() }
        DisposableEffect(activity, vm.roomInfo?.isPortrait == true, barsVisible) {
            val isPortrait = vm.roomInfo?.isPortrait == true
            val window = activity?.window
            val original = activity?.requestedOrientation
                ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val isPhoneDevice = configuration.smallestScreenWidthDp < 600
            if (activity != null && isPhoneDevice && !isPortrait) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            val controller = window?.let { WindowCompat.getInsetsController(window, view) }
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (barsVisible) controller?.show(WindowInsetsCompat.Type.systemBars())
            else controller?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                if (activity != null && isPhoneDevice && !isPortrait) {
                    activity.requestedOrientation = original
                }
            }
        }
    }

    // 播放 / 缓冲时保持屏幕常亮；出错后允许熄屏
    DisposableEffect(view, isBuffering, isPlaybackError, vm.error) {
        view.keepScreenOn = vm.error == null && !isPlaybackError
        onDispose { view.keepScreenOn = false }
    }

    // 触屏：菜单打开时系统返回先关菜单（电视端返回键在 onKeyDown/onKeyUp 里处理）
    BackHandler(enabled = menuOpen && !isTv) { closeMenu() }

    // ------------------------------------------------------------- 布局

    val isPortraitRoom = vm.roomInfo?.isPortrait == true
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (isTv) Modifier
                    .focusRequester(rootFocusRequester)
                    .focusable()
                    .onBvKeys(onKeyDown, onKeyUp)
                else Modifier
            )
    ) {
        // 画面：按视频比例居中
        BvVideoPlayer(
            modifier = Modifier
                .align(Alignment.Center)
                .then(if (isPortraitRoom) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
                .then(
                    if (isPortraitRoom) Modifier.aspectRatio(videoAspect, matchHeightConstraintsFirst = true)
                    else Modifier.aspectRatio(videoAspect)
                ),
            videoPlayer = videoPlayer,
            playerListener = playerListener,
        )

        // 弹幕层（透明度由引擎 config 处理）
        BvDanmakuPlayer(
            modifier = Modifier
                .align(Alignment.Center)
                .matchParentSize(),
            danmakuView = danmakuView,
        )

        // 触屏：点画面显隐控制层 / 关菜单
        if (!isTv) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (menuOpen) menuOpen = false else toggleControls()
                        })
                    }
            )
        }

        // 渐变 + 顶部信息栏 + 底部控制行
        AnimatedVisibility(
            visible = controlsVisible && !menuOpen,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                CinemaScrim(Modifier.fillMaxSize())
                LiveInfoBar(
                    nav = nav,
                    title = vm.roomInfo?.title.orEmpty(),
                    uname = vm.roomInfo?.uname.orEmpty(),
                    watchedText = watchedText,
                    popularity = popularity,
                    modifier = Modifier.align(Alignment.TopStart),
                )
                if (!isTv) {
                    LiveTouchBottomBar(
                        vm = vm,
                        danmakuEnabled = danmakuEnabled,
                        barsVisible = barsVisible,
                        onDanmakuToggle = { danmakuEnabled = !danmakuEnabled },
                        onRefresh = { vm.refreshStream() },
                        onOpenMenu = { menuOpen = true },
                        onToggleBars = { barsVisible = !barsVisible },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    )
                } else {
                    LiveTvBottomBar(
                        vm = vm,
                        danmakuEnabled = danmakuEnabled,
                        onDanmakuToggle = { danmakuEnabled = !danmakuEnabled },
                        onRefresh = { vm.refreshStream() },
                        onOpenMenu = { menuOpen = true },
                        onButtonsFocusChanged = { buttonsFocused = it },
                        buttonFocusRequester = buttonFocusRequester,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    )
                }
            }
        }

        // 右侧菜单：画质 / 线路 / 弹幕
        LiveMenuPanel(
            vm = vm,
            visible = menuOpen,
            isTv = isTv,
            danmakuEnabled = danmakuEnabled,
            danmakuFilterLevel = danmakuFilterLevel,
            onDanmakuToggle = { danmakuEnabled = !danmakuEnabled },
            onDanmakuFilterLevelChange = { level ->
                danmakuFilterLevel = level
                Prefs.defaultLiveDanmakuFilterLevel = level
            },
            onSwitchQuality = { qn ->
                pendingQualityToast = true
                vm.switchQuality(qn)
            },
            onSwitchLine = { index ->
                pendingLineToast = true
                vm.switchLine(index)
            },
            onClose = closeMenu,
            onDismissTouch = { menuOpen = false },
        )

        // 中央提示：加载失败 / 未开播 / 播放错误 / 缓冲
        LiveCenterTips(
            vm = vm,
            isBuffering = isBuffering,
            isPlaybackError = isPlaybackError,
            onRetryLoad = { vm.load(route.roomId) },
            onRetryStream = { vm.refreshStream() },
            onExit = onExit,
        )
    }
}

/**
 * 直播弹幕 append（必须在主线程；position 取播放器当前位置稍后一点，
 * 下一帧就会被引擎拾取上屏，且不会被当「迟到」丢弃）。
 */
private fun sendLiveDanmaku(danmakuView: DanmakuView?, videoPlayer: AbstractVideoPlayer, content: String) {
    if (danmakuView == null || content.isBlank()) return
    danmakuView.appendDanmakus(
        listOf(
            Danmaku(
                dmid = 0L,
                positionMs = videoPlayer.currentPosition.coerceAtLeast(0L).toInt() + 100,
                text = content,
                type = Danmaku.MODE_SCROLL,
                textSize = 25,
                color = android.graphics.Color.WHITE,
            )
        ),
        maxItems = 5000,
        alreadySorted = true,
    )
}

/** 电视端按键统一入口：KeyDown / KeyUp 都走 [toBvKey] 映射。 */
private fun Modifier.onBvKeys(
    onKeyDown: (BvKey) -> Boolean,
    onKeyUp: (BvKey) -> Boolean,
): Modifier = onPreviewKeyEvent { event ->
    val key = event.toBvKey() ?: return@onPreviewKeyEvent false
    when (event.type) {
        KeyEventType.KeyDown -> onKeyDown(key)
        KeyEventType.KeyUp -> onKeyUp(key)
        else -> false
    }
}

// ------------------------------------------------------------- 顶部信息栏

@Composable
private fun LiveInfoBar(
    nav: NavController,
    title: String,
    uname: String,
    watchedText: String?,
    popularity: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.live_player_back),
            onClick = { nav.popBackStack() },
        )
        LiveBadge()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = BvType.Title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (uname.isNotBlank()) {
                Text(
                    text = uname,
                    style = BvType.Small,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = liveStatsLine(watchedText, popularity),
            style = BvType.Small,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
        )
    }
}

@Composable
private fun liveStatsLine(watchedText: String?, popularity: Int): String {
    val parts = buildList {
        watchedText?.takeIf { it.isNotBlank() }?.let { add(it) }
        if (popularity > 0) add(stringResource(R.string.live_player_popularity, formatCount(popularity)))
    }
    return parts.joinToString(" · ")
}

// ------------------------------------------------------------- 底部控制行（触屏）

@Composable
private fun LiveTouchBottomBar(
    vm: LivePlayerViewModel,
    danmakuEnabled: Boolean,
    barsVisible: Boolean,
    onDanmakuToggle: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMenu: () -> Unit,
    onToggleBars: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.live_player_refresh),
            onClick = onRefresh,
        )
        BvChip(text = liveQualityLabel(vm), selected = false, onClick = onOpenMenu)
        BvChip(text = liveLineLabel(vm), selected = false, onClick = onOpenMenu)
        BvIconButton(
            icon = if (danmakuEnabled) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
            contentDescription = stringResource(R.string.live_player_menu_danmaku),
            onClick = onDanmakuToggle,
        )
        Spacer(Modifier.weight(1f))
        BvIconButton(
            icon = if (barsVisible) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = stringResource(R.string.live_player_fullscreen),
            onClick = onToggleBars,
        )
    }
}

// ------------------------------------------------------------- 底部控制行（电视）

@Composable
private fun LiveTvBottomBar(
    vm: LivePlayerViewModel,
    danmakuEnabled: Boolean,
    onDanmakuToggle: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMenu: () -> Unit,
    onButtonsFocusChanged: (Boolean) -> Unit,
    buttonFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .onFocusChanged { onButtonsFocusChanged(it.hasFocus) },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.live_player_refresh),
            onClick = onRefresh,
            modifier = Modifier.focusRequester(buttonFocusRequester),
        )
        BvChip(text = liveQualityLabel(vm), selected = false, onClick = onOpenMenu)
        BvChip(text = liveLineLabel(vm), selected = false, onClick = onOpenMenu)
        BvIconButton(
            icon = if (danmakuEnabled) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
            contentDescription = stringResource(R.string.live_player_menu_danmaku),
            onClick = onDanmakuToggle,
        )
    }
}

@Composable
private fun liveQualityLabel(vm: LivePlayerViewModel): String =
    vm.playInfo?.qualities?.firstOrNull { it.first == vm.currentQn }?.second
        ?: stringResource(R.string.live_player_menu_quality)

@Composable
private fun liveLineLabel(vm: LivePlayerViewModel): String =
    stringResource(R.string.live_player_line_label, vm.currentLineIndex + 1)

// ------------------------------------------------------------- 右侧菜单

@Composable
private fun LiveMenuPanel(
    vm: LivePlayerViewModel,
    visible: Boolean,
    isTv: Boolean,
    danmakuEnabled: Boolean,
    danmakuFilterLevel: Int,
    onDanmakuToggle: () -> Unit,
    onDanmakuFilterLevelChange: (Int) -> Unit,
    onSwitchQuality: (Int) -> Unit,
    onSwitchLine: (Int) -> Unit,
    onClose: () -> Unit,
    onDismissTouch: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            // 触屏：面板外点击关闭
            if (!isTv) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures(onTap = { onDismissTouch() }) }
                )
            }
            val menuFocus = remember { FocusRequester() }
            LaunchedEffect(isTv) {
                if (isTv) {
                    delay(16)
                    runCatching { menuFocus.requestFocus() }
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(340.dp),
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.live_player_menu_title),
                            style = BvType.PanelTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        BvIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.live_player_menu_close),
                            onClick = onClose,
                        )
                    }

                    Text(
                        text = stringResource(R.string.live_player_menu_quality),
                        style = BvType.Section,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val qualities = vm.playInfo?.qualities.orEmpty()
                    if (qualities.isEmpty()) {
                        Text(
                            text = stringResource(R.string.live_player_menu_empty),
                            style = BvType.Small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            qualities.forEachIndexed { index, (qn, desc) ->
                                BvChip(
                                    text = desc,
                                    selected = qn == vm.currentQn,
                                    onClick = { onSwitchQuality(qn) },
                                    modifier = if (index == 0) Modifier.focusRequester(menuFocus) else Modifier,
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.live_player_menu_line),
                        style = BvType.Section,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(vm.playInfo?.lines?.size ?: 0) { index ->
                            BvChip(
                                text = stringResource(R.string.live_player_line_label, index + 1),
                                selected = index == vm.currentLineIndex,
                                onClick = { onSwitchLine(index) },
                                modifier = if (index == 0 && qualities.isEmpty()) {
                                    Modifier.focusRequester(menuFocus)
                                } else Modifier,
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.live_player_menu_danmaku),
                        style = BvType.Section,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BvChip(
                        text = stringResource(
                            if (danmakuEnabled) R.string.live_player_danmaku_on
                            else R.string.live_player_danmaku_off
                        ),
                        selected = danmakuEnabled,
                        onClick = onDanmakuToggle,
                    )
                    // 按发送者用户等级过滤实时弹幕（历史弹幕没有等级信息，不过滤）
                    Text(
                        text = stringResource(R.string.live_player_danmaku_filter_level),
                        style = BvType.Section,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LiveDanmakuFilterLevelPresets.forEach { level ->
                            BvChip(
                                text = stringResource(R.string.live_player_danmaku_filter_level_value, level),
                                selected = level == danmakuFilterLevel,
                                onClick = { onDanmakuFilterLevelChange(level) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- 中央提示

@Composable
private fun LiveCenterTips(
    vm: LivePlayerViewModel,
    isBuffering: Boolean,
    isPlaybackError: Boolean,
    onRetryLoad: () -> Unit,
    onRetryStream: () -> Unit,
    onExit: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            vm.error != null -> {
                LiveTipCard {
                    Text(
                        text = stringResource(R.string.live_player_load_failed, vm.error ?: ""),
                        style = BvType.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BvButton(text = stringResource(R.string.live_player_retry), onClick = onRetryLoad)
                        BvButton(
                            text = stringResource(R.string.live_player_back),
                            onClick = onExit,
                            style = BvButtonStyle.Outlined,
                        )
                    }
                }
            }

            vm.roomInfo != null && !vm.playing -> {
                LiveTipCard {
                    Text(
                        text = stringResource(R.string.live_player_not_living),
                        style = BvType.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    BvButton(text = stringResource(R.string.live_player_back), onClick = onExit)
                }
            }

            isPlaybackError -> {
                LiveTipCard {
                    Text(
                        text = stringResource(R.string.live_player_play_failed),
                        style = BvType.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    BvButton(
                        text = stringResource(R.string.live_player_retry),
                        onClick = onRetryStream,
                        icon = Icons.Rounded.Refresh,
                    )
                }
            }

            vm.loading || isBuffering -> LoadingIndicator(modifier = Modifier.size(120.dp))
        }
    }
}

@Composable
private fun LiveTipCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
