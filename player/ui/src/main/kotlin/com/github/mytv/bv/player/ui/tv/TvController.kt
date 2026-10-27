@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package com.github.mytv.bv.player.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mytv.bv.player.entity.LocalVideoPlayerConfigData
import com.github.mytv.bv.player.entity.LocalVideoPlayerDebugInfoData
import com.github.mytv.bv.player.entity.LocalVideoPlayerHistoryData
import com.github.mytv.bv.player.entity.LocalVideoPlayerLogsData
import com.github.mytv.bv.player.entity.LocalVideoPlayerPaymentData
import com.github.mytv.bv.player.entity.LocalVideoPlayerSeekData
import com.github.mytv.bv.player.entity.LocalVideoPlayerStateData
import com.github.mytv.bv.player.entity.LocalVideoPlayerVideoInfoData
import com.github.mytv.bv.player.entity.LocalVideoPlayerVideoShotData
import com.github.mytv.bv.player.entity.PlayerLongPressAction
import com.github.mytv.bv.player.shared.BuildConfig
import com.github.mytv.bv.player.ui.BottomSubtitle
import com.github.mytv.bv.player.ui.BufferingTip
import com.github.mytv.bv.player.ui.KeyedSideEffect
import com.github.mytv.bv.player.ui.PaidRequireTip
import com.github.mytv.bv.player.ui.PlayErrorTip
import com.github.mytv.bv.player.ui.PlayerController
import com.github.mytv.bv.player.ui.PreviewTip
import com.github.mytv.bv.player.ui.R
import com.github.mytv.bv.player.ui.SeekPreviewRow
import com.github.mytv.bv.player.ui.design.BvKey
import com.github.mytv.bv.player.ui.design.CinemaScrim
import com.github.mytv.bv.player.ui.design.LocalPlayerDpad
import com.github.mytv.bv.player.ui.design.PlayerType
import com.github.mytv.bv.player.ui.design.WavyProgress
import com.github.mytv.bv.player.ui.design.bvFocus
import com.github.mytv.bv.player.ui.design.onBvKey
import com.github.mytv.bv.player.ui.design.onBvKeyUp
import com.github.mytv.bv.player.ui.nextAfter
import com.github.mytv.bv.player.ui.rememberPartLabel
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.formatHourMinSec
import com.github.mytv.bv.util.requestFocus
import com.github.mytv.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.github.mytv.bv.player.shared.R as SharedR

/** 控制层显示后自动淡出的时间（第 9.4 节：3 秒）。 */
private const val AutoHideMillis = 3000L

private val TimeStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = PlayerType.TabularNum,
)
private val SubtitleLineStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)

/** 菜单 / 选集面板打开时，控制层右边让出的宽度（第 9.4 节：end 340）。 */
private val ControlsEndWithPanel = 340.dp
private val ControlsEnd = 48.dp

/**
 * 电视端控制层：由旧 player/tv 的 VideoPlayerController 改写。按键表照搬旧版：
 *
 * | 状态 | 按键 | 行为 |
 * |---|---|---|
 * | 菜单 / 选集打开 | 返回、菜单（抬起） | 关闭面板 |
 * | 快进中 | 确认（抬起） | 跳到目标时间，暂停中则继续播放 |
 * | 快进中 | 返回、菜单、上、下（抬起） | 取消快进 |
 * | — | 确认短按（抬起） | 有「回到上次进度」提示时跳转，否则播放 / 暂停 |
 * | — | 确认长按（按住） | 按设置打开菜单，或临时加速播放（松开恢复） |
 * | — | 菜单（抬起） | 打开菜单 |
 * | — | 上（抬起） | 打开选集 |
 * | — | 下（抬起） | 显示控制栏并聚焦按钮行（旧版为显示视频信息） |
 * | — | 左右、快退快进（按下，可连按加速） | 快进 / 快退 |
 * | — | 返回（抬起） | 暂停中直接退出；播放中 3 秒内连按两次退出 |
 * | — | 播放/暂停媒体键（抬起） | 播放 / 暂停 |
 *
 * 焦点在控制栏按钮行时：左右在按钮间移动、确认点击按钮；上 / 返回回到画面；菜单键打开菜单。
 * 任意方向键 / 确认键后控制层出现，3 秒后淡出（打开面板、快进中不淡出）。
 */
@Composable
internal fun TvController(
    modifier: Modifier,
    controller: PlayerController,
    ownerName: String,
    onOpenComments: (() -> Unit)?,
    showRemoteControlHint: Boolean,
    onRemoteControlHintDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = remember { KotlinLogging.logger("TvController") }
    val seekData = LocalVideoPlayerSeekData.current
    val stateData = LocalVideoPlayerStateData.current
    val configData = LocalVideoPlayerConfigData.current
    val historyData = LocalVideoPlayerHistoryData.current
    val videoInfoData = LocalVideoPlayerVideoInfoData.current
    val videoShotData = LocalVideoPlayerVideoShotData.current
    val paymentData = LocalVideoPlayerPaymentData.current
    val logsData = LocalVideoPlayerLogsData.current
    val debugInfoData = LocalVideoPlayerDebugInfoData.current
    val menuState = controller.menuState
    val videoPlayer = controller.videoPlayer

    val rootFocusRequester = remember { FocusRequester() }
    val buttonRowFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    var controlsVisible by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var buttonsFocused by remember { mutableStateOf(false) }

    var menuOpen by remember { mutableStateOf(false) }
    var menuCategory by remember { mutableStateOf(TvMenuCategory.Picture) }
    var listOpen by remember { mutableStateOf(false) }
    var panelOpenedFromButtons by remember { mutableStateOf(false) }

    var seeking by remember { mutableStateOf(false) }
    var goTime by remember { mutableLongStateOf(0L) }
    var seekChangeCount by remember { mutableIntStateOf(0) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }

    var centerLongFired by remember { mutableStateOf(false) }
    // 长按确认键加速：centerLongHeld 防止按住期间系统重复上报长按事件时反复触发
    var centerLongHeld by remember { mutableStateOf(false) }
    var longPressSpeedUp by remember { mutableStateOf(false) }
    var lastPressBack by remember { mutableLongStateOf(0L) }
    var showLogs by remember { mutableStateOf(false) }

    val panelOpen = menuOpen || listOpen

    // ------------------------------------------------------------- 控制层显示

    val showControls: () -> Unit = {
        controlsVisible = true
        interactionTick++
    }

    val hideControls: () -> Unit = {
        if (buttonsFocused) rootFocusRequester.requestFocus(scope)
        controlsVisible = false
    }

    val focusButtons: () -> Unit = {
        showControls()
        scope.launch {
            // 等控制层组合完成
            delay(16)
            runCatching { buttonRowFocusRequester.requestFocus() }
                .onFailure {
                    delay(100)
                    runCatching { buttonRowFocusRequester.requestFocus() }
                }
        }
    }

    val openMenu: (TvMenuCategory) -> Unit = { category ->
        panelOpenedFromButtons = buttonsFocused
        listOpen = false
        menuCategory = category
        menuOpen = true
        showControls()
    }

    val openList: () -> Unit = {
        panelOpenedFromButtons = buttonsFocused
        menuOpen = false
        listOpen = true
        showControls()
    }

    val closePanels: () -> Unit = {
        logger.fInfo { "hide all panels" }
        menuOpen = false
        listOpen = false
        seeking = false
        if (panelOpenedFromButtons && controlsVisible) {
            buttonRowFocusRequester.requestFocus(scope)
        } else {
            rootFocusRequester.requestFocus(scope)
        }
        interactionTick++
    }

    // 系统返回不一定经过遥控按键事件；复用关闭面板逻辑，避免直接退出播放页。
    BackHandler(enabled = panelOpen && !showRemoteControlHint) {
        closePanels()
    }

    if (controlsVisible && !panelOpen && !seeking) {
        LaunchedEffect(interactionTick) {
            delay(AutoHideMillis)
            hideControls()
        }
    }

    // ------------------------------------------------------------- 快进（照搬旧版加速逻辑）

    val openSeek: () -> Unit = {
        if (!seeking) goTime = seekData.position
        seeking = true
        showControls()
    }

    val calCoefficient: () -> Int = {
        if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }
    }

    val onTimeForward: () -> Unit = {
        val targetTime = goTime + (10000 + calCoefficient() * 5000)
        goTime = if (targetTime > seekData.duration) seekData.duration else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeForward: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }

    val onTimeBack: () -> Unit = {
        val targetTime = goTime - (5000 + calCoefficient() * 5000)
        goTime = if (targetTime < 0) 0 else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeBack: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }

    val togglePlay: () -> Unit = {
        if (videoPlayer.isPlaying) controller.pause() else controller.play()
    }

    // ------------------------------------------------------------- 长按加速

    val startLongPressSpeedUp: () -> Unit = {
        if (!longPressSpeedUp) {
            longPressSpeedUp = true
            controller.setTemporarySpeed(configData.longPressSpeed)
            logger.fInfo { "Long press speed up: ${configData.longPressSpeed}x" }
        }
    }

    /** 恢复为设置里的倍速；幂等，松开 / 暂停 / 换集 / 退出时都调用。 */
    val restoreLongPressSpeed: () -> Unit = {
        if (longPressSpeedUp) {
            longPressSpeedUp = false
            controller.setTemporarySpeed(null)
            logger.fInfo { "Long press speed restored: ${configData.currentVideoSpeed}x" }
        }
    }

    // ------------------------------------------------------------- 按键（按下）

    val onKeyDown: (BvKey) -> Boolean = handler@{ key ->
        if (showRemoteControlHint) return@handler false
        if (panelOpen) {
            interactionTick++
            return@handler key == BvKey.Back || key == BvKey.Menu
        }
        if (buttonsFocused) {
            interactionTick++
            return@handler when (key) {
                BvKey.Back, BvKey.Menu, BvKey.Up, BvKey.Down, BvKey.PlayPause, BvKey.CenterLong -> true
                BvKey.FastForward, BvKey.Rewind -> {
                    // 快进时焦点回到画面，之后的左右 / 确认键按快进处理
                    rootFocusRequester.requestFocus(scope)
                    openSeek()
                    if (key == BvKey.FastForward) onTimeForward() else onTimeBack()
                    true
                }

                else -> false
            }
        }
        if (seeking) {
            interactionTick++
            return@handler when (key) {
                BvKey.Left, BvKey.Rewind -> {
                    onTimeBack(); true
                }

                BvKey.Right, BvKey.FastForward -> {
                    onTimeForward(); true
                }

                BvKey.Back, BvKey.Menu, BvKey.Up, BvKey.Down, BvKey.Center, BvKey.CenterLong -> true
                else -> false
            }
        }
        when (key) {
            BvKey.Center -> {
                showControls()
                true
            }

            BvKey.CenterLong -> {
                logger.fInfo { "[$key] long press" }
                centerLongFired = true
                // 按住期间系统可能重复上报长按，只在第一次按下时处理
                if (!centerLongHeld) {
                    centerLongHeld = true
                    if (configData.longPressAction == PlayerLongPressAction.SpeedUp) {
                        startLongPressSpeedUp()
                    } else {
                        openMenu(TvMenuCategory.Picture)
                    }
                }
                true
            }

            BvKey.Up, BvKey.Down -> {
                showControls()
                true
            }

            BvKey.Menu, BvKey.Back, BvKey.PlayPause -> true

            BvKey.Left, BvKey.Rewind -> {
                openSeek()
                onTimeBack()
                true
            }

            BvKey.Right, BvKey.FastForward -> {
                openSeek()
                onTimeForward()
                true
            }

            else -> false
        }
    }

    // ------------------------------------------------------------- 按键（抬起）

    val onKeyUp: (BvKey) -> Boolean = handler@{ key ->
        if (showRemoteControlHint) return@handler false
        // 部分盒子的长按确认键（763）抬起时仍是 CenterLong
        if (key == BvKey.CenterLong) {
            centerLongFired = false
            centerLongHeld = false
            restoreLongPressSpeed()
            return@handler true
        }
        var wasLongPress = false
        if (key == BvKey.Center) {
            wasLongPress = centerLongFired
            centerLongFired = false
            centerLongHeld = false
            restoreLongPressSpeed()
        }
        if (panelOpen) {
            if (key == BvKey.Back || key == BvKey.Menu) {
                closePanels()
                return@handler true
            }
            return@handler false
        }
        if (buttonsFocused) {
            return@handler when (key) {
                BvKey.Back -> {
                    hideControls()
                    true
                }

                BvKey.Up -> {
                    rootFocusRequester.requestFocus(scope)
                    true
                }

                BvKey.Menu -> {
                    openMenu(TvMenuCategory.Picture)
                    true
                }

                BvKey.PlayPause -> {
                    togglePlay()
                    true
                }

                BvKey.Down -> true
                else -> false
            }
        }
        if (seeking) {
            return@handler when (key) {
                BvKey.Center -> {
                    controller.seekTo(goTime)
                    if (!videoPlayer.isPlaying) controller.play()
                    seeking = false
                    true
                }

                BvKey.Back, BvKey.Menu, BvKey.Up, BvKey.Down -> {
                    seeking = false
                    true
                }

                else -> false
            }
        }
        when (key) {
            BvKey.Center -> {
                if (wasLongPress) return@handler true
                if (stateData.showBackToHistory) {
                    controller.backToHistory()
                    return@handler true
                }
                logger.fInfo { "[$key] short press" }
                togglePlay()
                true
            }

            BvKey.Up -> {
                openList()
                true
            }

            BvKey.Down -> {
                focusButtons()
                true
            }

            BvKey.Menu -> {
                if (menuOpen) closePanels() else openMenu(TvMenuCategory.Picture)
                true
            }

            BvKey.Back -> {
                if (!videoPlayer.isPlaying) {
                    logger.fInfo { "Exiting video player" }
                    controller.exit()
                    return@handler true
                }
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastPressBack < 1000 * 3) {
                    logger.fInfo { "Exiting video player" }
                    controller.exit()
                } else {
                    lastPressBack = currentTime
                    SharedR.string.video_player_press_back_again_to_exit.toast(context)
                }
                true
            }

            BvKey.PlayPause -> {
                togglePlay()
                true
            }

            else -> false
        }
    }

    // ------------------------------------------------------------- 副作用

    // 进入播放器、以及遥控器操作提示关闭后，焦点回到画面
    LaunchedEffect(showRemoteControlHint) {
        if (!showRemoteControlHint) rootFocusRequester.requestFocus(scope)
    }

    LaunchedEffect(logsData.logs) {
        showLogs = true
        delay(3000)
        showLogs = false
    }

    // 暂停 / 换集时恢复原倍速（长按期间若视频暂停或切集，不留下加速状态）
    KeyedSideEffect(stateData.isPlaying) {
        if (!stateData.isPlaying) restoreLongPressSpeed()
    }
    KeyedSideEffect(configData.currentVideoCid) {
        restoreLongPressSpeed()
    }
    DisposableEffect(Unit) {
        onDispose {
            if (longPressSpeedUp) {
                longPressSpeedUp = false
                runCatching { controller.setTemporarySpeed(null) }
            }
        }
    }

    // ------------------------------------------------------------- 文案

    val nextItem = remember(configData.availableVideoList, configData.currentVideoCid) {
        configData.availableVideoList.nextAfter(configData.currentVideoCid)
    }
    val partLabel = rememberPartLabel()
    val titleLine = partLabel ?: videoInfoData.title
    val subtitleLine = when {
        partLabel != null && ownerName.isNotBlank() ->
            stringResource(R.string.player_ui_title_join, videoInfoData.title, ownerName)

        partLabel != null -> videoInfoData.title
        else -> ownerName
    }

    val controlsEnd by animateDpAsState(
        targetValue = if (panelOpen) ControlsEndWithPanel else ControlsEnd,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tvControlsEnd",
    )
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(rootFocusRequester)
                .onBvKey(onKeyDown)
                .onBvKeyUp(onKeyUp)
                .focusable()
        ) {
            content()

            if (BuildConfig.DEBUG) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = debugInfoData.debugInfo,
                        style = PlayerType.Meta,
                    )
                }
            }

            BottomSubtitle()

            // 长按确认键加速中的提示
            if (longPressSpeedUp) {
                TvLongPressSpeedTip(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    speed = configData.longPressSpeed,
                )
            }

            // 播放状态提示（旧 PlayStateTips）
            if (!stateData.isPlaying && !stateData.isBuffering && !stateData.isError && !controlsVisible) {
                PauseIcon(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                )
            }
            if (stateData.isBuffering && !stateData.isError) {
                BufferingTip(modifier = Modifier.align(Alignment.Center))
            }
            if (stateData.isError) {
                PlayErrorTip(
                    modifier = Modifier.align(Alignment.Center),
                    exception = stateData.exception,
                )
            }
            if (paymentData.isPreviewPlayback) {
                PreviewTip(
                    show = paymentData.showPreviewTip,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (paymentData.needPay) {
                PaidRequireTip(
                    modifier = Modifier.align(Alignment.Center),
                    epid = paymentData.epid,
                )
            }

            // 控制层
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                // 进度只在控制层可见时读取，隐藏时不跟随 100ms 轮询重组
                val shownPosition = if (seeking) goTime else seekData.position
                val safeDuration = seekData.duration.coerceAtLeast(1L)
                Box(modifier = Modifier.fillMaxSize()) {
                    CinemaScrim()

                    // 左上 at (48, 27)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .controlsPadding(start = 48.dp, top = 27.dp, end = { controlsEnd })
                    ) {
                        Text(
                            text = titleLine,
                            style = PlayerType.PanelTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitleLine.isNotBlank()) {
                            Text(
                                text = subtitleLine,
                                style = SubtitleLineStyle,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // 底部 at (start 48, end 340 / 48, bottom 27)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .controlsPadding(start = 48.dp, end = { controlsEnd }, bottom = 27.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SeekPreviewRow(
                            // 缩略图底边与时间行之间留 24dp
                            bottomGap = 14.dp,
                            visible = seeking,
                            fraction = goTime / safeDuration.toFloat(),
                            videoShot = videoShotData.videoShot,
                            position = goTime,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = shownPosition.formatHourMinSec(), style = TimeStyle)
                            Text(text = seekData.duration.formatHourMinSec(), style = TimeStyle)
                        }
                        WavyProgress(
                            progress = { shownPosition / safeDuration.toFloat() },
                            waving = stateData.isPlaying && !seeking,
                        )
                        Row(
                            modifier = Modifier
                                .focusRequester(buttonRowFocusRequester)
                                .focusRestorer(playPauseFocusRequester)
                                .onFocusChanged { buttonsFocused = it.hasFocus }
                                .focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TvControlButton(
                                modifier = Modifier.focusRequester(playPauseFocusRequester),
                                icon = if (stateData.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(
                                    if (stateData.isPlaying) R.string.player_ui_cd_pause else R.string.player_ui_cd_play
                                ),
                                onClick = togglePlay,
                            )
                            TvControlButton(
                                icon = Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.player_ui_cd_next),
                                enabled = nextItem != null,
                                onClick = { nextItem?.let { controller.playNewVideo(it) } },
                            )
                            TvControlButton(
                                icon = Icons.Filled.Subscriptions,
                                contentDescription = stringResource(R.string.player_ui_episodes),
                                enabled = configData.availableVideoList.isNotEmpty(),
                                onClick = openList,
                            )
                            if (onOpenComments != null) {
                                TvControlButton(
                                    icon = Icons.Filled.ChatBubble,
                                    contentDescription = stringResource(R.string.player_ui_cd_comments),
                                    onClick = onOpenComments,
                                )
                            }
                            TvControlButton(
                                icon = Icons.Filled.Subtitles,
                                contentDescription = stringResource(R.string.player_ui_cd_subtitle),
                                onClick = { openMenu(TvMenuCategory.Subtitle) },
                            )
                            TvControlButton(
                                icon = Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.player_ui_cd_settings),
                                onClick = { openMenu(TvMenuCategory.Picture) },
                            )
                        }
                    }
                }
            }

            TvBackToHistoryTip(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = if (controlsVisible) 150.dp else 32.dp),
                show = stateData.showBackToHistory,
                time = historyData.lastPlayed.toLong().formatHourMinSec(),
            )

            if (showLogs && logsData.logs.isNotBlank()) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(text = logsData.logs, style = PlayerType.Meta)
                }
            }

            // 菜单面板
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterEnd),
                visible = menuOpen,
                enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                TvMenuPanel(
                    menuState = menuState,
                    category = menuCategory,
                    onCategoryChange = { menuCategory = it },
                )
            }

            // 选集面板
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterEnd),
                visible = listOpen,
                enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                TvVideoListPanel(
                    menuState = menuState,
                    onPlayNewVideo = controller.playNewVideo,
                )
            }

            if (showRemoteControlHint) {
                RemoteControlHintOverlay(onDismiss = onRemoteControlHintDismiss)
            }
        }
    }
}

/** 动画边距在测量阶段读取，避免侧栏展开时逐帧重组控制层。 */
private fun Modifier.controlsPadding(
    start: Dp,
    end: () -> Dp,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
): Modifier = layout { measurable, constraints ->
    val startPx = start.roundToPx()
    val endPx = end().roundToPx().coerceAtLeast(0)
    val topPx = top.roundToPx()
    val bottomPx = bottom.roundToPx()
    val horizontal = startPx + endPx
    val vertical = topPx + bottomPx
    val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))
    layout(
        constraints.constrainWidth(placeable.width + horizontal),
        constraints.constrainHeight(placeable.height + vertical),
    ) {
        placeable.placeRelative(startPx, topPx)
    }
}

/**
 * 控制栏按钮：44dp FilledTonal 图标按钮（原型 `.m-ib.t`：surfaceContainerHighest / onSurface），
 * 聚焦时放大 1.10 + 外侧描边，背景 inverseSurface、内容色 inverseOnSurface。
 */
@Composable
private fun TvControlButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 44.dp,
) {
    val dpad = LocalPlayerDpad.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val showFocus = focused && dpad
    val colorScheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Color>()
    val container by animateColorAsState(
        if (showFocus) colorScheme.inverseSurface else colorScheme.surfaceContainerHighest,
        effects,
        label = "tvButtonContainer",
    )
    val content by animateColorAsState(
        if (showFocus) colorScheme.inverseOnSurface else colorScheme.onSurface,
        effects,
        label = "tvButtonContent",
    )
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier
            .bvFocus(dpad = dpad)
            .size(size),
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = container,
            contentColor = content,
        ),
        interactionSource = interactionSource,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
