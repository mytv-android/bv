@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.touch

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoShotData
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.ui.BottomSubtitle
import dev.aaa1115910.bv.player.ui.BufferingTip
import dev.aaa1115910.bv.player.ui.PaidRequireTip
import dev.aaa1115910.bv.player.ui.PlayErrorTip
import dev.aaa1115910.bv.player.ui.PlayerController
import dev.aaa1115910.bv.player.ui.PreviewTip
import dev.aaa1115910.bv.player.ui.R
import dev.aaa1115910.bv.player.ui.formatSpeed
import dev.aaa1115910.bv.player.ui.nextAfter
import dev.aaa1115910.bv.player.ui.rememberPartLabel
import dev.aaa1115910.bv.util.formatHourMinSec
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TAG = "TouchController"

/** 控制层无操作后自动淡出的时间（第 9.4 节：3 秒）。 */
private const val AutoHideMillis = 3000L

/**
 * 触摸端控制层：由旧 player/mobile 的 BvPlayerController 改写。
 * - 手势沿用旧版：单击显示/隐藏；双击暂停；长按 2 倍速；横滑快进；左半竖滑亮度、右半竖滑音量；
 * - 控制层单击出现，播放中 3 秒无操作淡出（defaultEffectsSpec）；
 * - 菜单一律从右侧滑出侧边面板；
 * - 补齐字幕（BottomSubtitle + 字幕面板）、画面比例、弹幕蒙版。
 */
@Composable
internal fun TouchController(
    modifier: Modifier,
    controller: PlayerController,
    isTablet: Boolean,
    isFullScreen: Boolean,
    onEnterFullScreen: () -> Unit,
    onExitFullScreen: () -> Unit,
    onEmbeddedMore: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit,
    zoomState: VideoZoomState? = null,
) {
    val context = LocalContext.current
    val seekData = LocalVideoPlayerSeekData.current
    val stateData = LocalVideoPlayerStateData.current
    val configData = LocalVideoPlayerConfigData.current
    val videoInfoData = LocalVideoPlayerVideoInfoData.current
    val videoShotData = LocalVideoPlayerVideoShotData.current
    val historyData = LocalVideoPlayerHistoryData.current
    val paymentData = LocalVideoPlayerPaymentData.current
    val menuState = controller.menuState

    var showControls by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf(TouchPanel.None) }
    var lastPanel by remember { mutableStateOf(TouchPanel.None) }
    var progressDragFraction by remember { mutableStateOf<Float?>(null) }

    var is2xPlaying by remember { mutableStateOf(false) }
    var isMovingSeek by remember { mutableStateOf(false) }
    var moveStartTime by remember { mutableLongStateOf(0L) }
    var moveMs by remember { mutableLongStateOf(0L) }

    var isMovingBrightness by remember { mutableStateOf(false) }
    var movedBrightness by remember { mutableStateOf(false) }
    var moveStartBrightness by remember { mutableFloatStateOf(0f) }
    var currentBrightnessProgress by remember { mutableFloatStateOf(0f) }

    var isMovingVolume by remember { mutableStateOf(false) }
    var moveStartVolume by remember { mutableIntStateOf(0) }
    var currentVolumeProgress by remember { mutableFloatStateOf(0f) }

    val poke: () -> Unit = { interactionTick++ }

    val openPanel: (TouchPanel) -> Unit = { target ->
        lastPanel = target
        panel = target
        poke()
    }
    val closePanel: () -> Unit = { panel = TouchPanel.None }

    val playPause: () -> Unit = {
        poke()
        if (stateData.isPlaying) controller.pause() else controller.play()
    }

    LaunchedEffect(isFullScreen) {
        if (!isFullScreen) panel = TouchPanel.None
    }

    // 控制层：播放中 3 秒无操作淡出；拖动进度、打开面板时不淡出
    LaunchedEffect(showControls, interactionTick, stateData.isPlaying, panel, progressDragFraction) {
        if (showControls && stateData.isPlaying && panel == TouchPanel.None && progressDragFraction == null) {
            delay(AutoHideMillis)
            showControls = false
        }
    }

    LaunchedEffect(is2xPlaying) {
        if (is2xPlaying) showControls = false
    }

    BackHandler(enabled = panel != TouchPanel.None) { closePanel() }

    // ------------------------------------------------------------- 手势回调（逻辑照搬旧版）

    val onTap: () -> Unit = {
        Log.i(TAG, "Screen tap")
        if (panel != TouchPanel.None) {
            closePanel()
            showControls = true
            poke()
        } else {
            if (!is2xPlaying) showControls = !showControls
            poke()
        }
    }

    val onLongPress: () -> Unit = {
        Log.i(TAG, "Screen long press")
        is2xPlaying = true
        controller.setTemporarySpeed(2f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = context.getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }

    val onLongPressEnd: (Float) -> Unit = {
        Log.i(TAG, "Screen long press end")
        is2xPlaying = false
        controller.setTemporarySpeed(null)
    }

    val onDoubleTap: () -> Unit = {
        Log.i(TAG, "Screen double tap, isPlaying: ${stateData.isPlaying}")
        if (stateData.isPlaying) controller.pause() else controller.play()
    }

    val onHorizontalDrag: (Float) -> Unit = { move ->
        if (!isMovingSeek) moveStartTime = seekData.position
        isMovingSeek = true
        moveMs = move.toLong() * 50
    }

    val onMovingBrightness: (Float) -> Unit = { move ->
        val activity = context.findActivity()
        if (activity != null) {
            val window = activity.window
            val attr = window.attributes
            if (!isMovingBrightness) {
                // Settings.System.SCREEN_BRIGHTNESS [0, 255]
                // attr.screenBrightness [0, 1f]
                val oldBrightness = if (movedBrightness) attr.screenBrightness else {
                    Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS
                    ) / 255f
                }
                moveStartBrightness = oldBrightness
                isMovingBrightness = true
                movedBrightness = true
            }
            val newBrightness = (moveStartBrightness - move / 600).coerceIn(0f, 1f)
            attr.screenBrightness = newBrightness
            window.attributes = attr
            currentBrightnessProgress = newBrightness
        }
    }

    val onMovingVolume: (Float) -> Unit = { move ->
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (!isMovingVolume) {
            moveStartVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            isMovingVolume = true
        }
        val step = maxVolume.toFloat() / 600f
        val newVolume = ((moveStartVolume - move * step).roundToInt()).coerceIn(0, maxVolume)
        currentVolumeProgress = newVolume / maxVolume.toFloat()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    val nextItem: VideoListItemData? = remember(configData.availableVideoList, configData.currentVideoCid) {
        configData.availableVideoList.nextAfter(configData.currentVideoCid)
    }
    val partLabel = rememberPartLabel()
    val title = if (partLabel != null && videoInfoData.title.isNotBlank()) {
        stringResource(R.string.player_ui_title_join, partLabel, videoInfoData.title)
    } else {
        videoInfoData.title
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            content()

            BottomSubtitle()

            // 手势层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .detectPlayerGestures(
                        enableSafetyArea = isFullScreen,
                        currentSpeed = configData.currentVideoSpeed,
                        onTap = onTap,
                        onLongPress = onLongPress,
                        onLongPressEnd = onLongPressEnd,
                        onDoubleTap = onDoubleTap,
                        onVolumeDrag = onMovingVolume,
                        onBrightnessDrag = onMovingBrightness,
                        onSeekDrag = onHorizontalDrag,
                        onDragEnd = { volumeMove, brightnessMove, seekMove ->
                            if (volumeMove != 0f) {
                                isMovingVolume = false
                            } else if (brightnessMove != 0f) {
                                isMovingBrightness = false
                            } else {
                                isMovingSeek = false
                                val seekMoveMs = seekMove.toLong() * 50
                                controller.seekTo((moveStartTime + seekMoveMs).coerceAtLeast(0L))
                                Log.i(TAG, "Seek move $seekMoveMs")
                            }
                        },
                        zoomState = zoomState,
                    )
            )

            if (stateData.isBuffering && !stateData.isError) {
                BufferingTip(modifier = Modifier.align(Alignment.Center))
            }
            if (stateData.isError) {
                PlayErrorTip(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    exception = stateData.exception,
                )
            }
            // 试看可播时短暂提示；完全不可播（needPay 且无流）才显示购买提示
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

            SeekMoveTip(
                show = isMovingSeek,
                startTime = moveStartTime,
                move = moveMs,
                totalTime = seekData.duration
            )
            BrightnessTip(show = isMovingBrightness, progress = currentBrightnessProgress)
            VolumeTip(show = isMovingVolume, progress = currentVolumeProgress)
            QuickDoubleSpeedPlaybackTip(show = is2xPlaying)

            if (stateData.showBackToHistory && isFullScreen) {
                BackToHistoryTip(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = if (showControls) 96.dp else 32.dp),
                    time = historyData.lastPlayed.toLong().formatHourMinSec(),
                    onClick = controller.backToHistory,
                )
            }

            AnimatedVisibility(
                visible = showControls || panel != TouchPanel.None,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                if (isFullScreen) {
                    FullscreenControls(
                        isTablet = isTablet,
                        panelOpen = panel != TouchPanel.None,
                        title = title,
                        isPlaying = stateData.isPlaying,
                        position = seekData.position,
                        duration = seekData.duration,
                        danmakuEnabled = menuState.danmakuEnabled,
                        showEpisodes = configData.availableVideoList.size > 1,
                        hasNext = nextItem != null,
                        speedText = formatSpeed(context, menuState.speed),
                        resolutionText = menuState.resolution.getDisplayName(context),
                        videoShot = videoShotData.videoShot,
                        dragFraction = progressDragFraction,
                        onDragFractionChange = {
                            progressDragFraction = it
                            poke()
                        },
                        onBack = onExitFullScreen,
                        onOpenSubtitle = { openPanel(TouchPanel.Subtitle) },
                        onToggleDanmaku = {
                            poke()
                            menuState.setDanmakuEnabled(!menuState.danmakuEnabled)
                        },
                        onOpenMore = { openPanel(TouchPanel.More) },
                        onPlayPause = playPause,
                        onNext = { nextItem?.let { controller.playNewVideo(it) } },
                        onOpenEpisodes = { openPanel(TouchPanel.Episodes) },
                        onOpenSpeed = { openPanel(TouchPanel.Speed) },
                        onOpenQuality = { openPanel(TouchPanel.Quality) },
                        onExitFullScreen = onExitFullScreen,
                        onSeek = { position ->
                            poke()
                            controller.seekTo(position)
                        },
                    )
                } else {
                    EmbeddedControls(
                        isTablet = isTablet,
                        isPlaying = stateData.isPlaying,
                        position = seekData.position,
                        duration = seekData.duration,
                        onBack = controller.exit,
                        onMore = onEmbeddedMore,
                        onPlayPause = playPause,
                        onEnterFullScreen = onEnterFullScreen,
                        onSeek = { position ->
                            poke()
                            controller.seekTo(position)
                        },
                        onDragFractionChange = {
                            progressDragFraction = it
                            poke()
                        },
                        dragFraction = progressDragFraction,
                    )
                }
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterEnd),
                visible = panel != TouchPanel.None && isFullScreen,
                enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { it } +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                TouchSidePanel(
                    panel = lastPanel,
                    menuState = menuState,
                    onPlayNewVideo = controller.playNewVideo,
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
