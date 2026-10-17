@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui

import android.os.CountDownTimer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.AkDanmakuPlayer
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.RequestState
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.VideoPlayerStateData
import dev.aaa1115910.bv.player.ui.design.LocalPlayerDpad
import dev.aaa1115910.bv.player.ui.design.LocalPlayerReduceMotion
import dev.aaa1115910.bv.player.ui.design.rememberDpadMode
import dev.aaa1115910.bv.player.ui.touch.TouchController
import dev.aaa1115910.bv.player.ui.tv.TvController
import dev.aaa1115910.bv.player.util.danmakuMask
import dev.aaa1115910.bv.util.countDownTimer
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.timeTask
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import kotlin.math.max

/**
 * 统一播放器入口（第 11 节）：合并旧 `player/mobile` 与 `player/tv` 的 `BvPlayer`。
 * 内部 `if (isTv) TvController(...) else TouchController(...)`。
 *
 * 本组件**不安装主题**：调用方负责在外层包 `BvTheme(forceDark = true) { CoverTheme(cover, forceDark = true) { … } }`。
 * 和旧版一样，播放数据通过 `LocalVideoPlayer*Data` 系列 CompositionLocal 由调用方提供
 * （VideoInfo / Logs / History / Payment / LoadState / Config / DanmakuMasks / VideoShot / SeekThumb）。
 *
 * @param videoPlayer 播放内核（VideoPlayerV3ViewModel.videoPlayer）
 * @param danmakuPlayer 弹幕播放器（VideoPlayerV3ViewModel.danmakuPlayer）
 * @param isTv 电视布局 + 遥控器按键逻辑；false 时为触摸布局
 * @param isTablet 触摸布局使用平板尺寸（底部控制区宽 800，侧边面板 340）
 * @param isFullScreen 触摸端：全屏控制层；false 时为详情页内嵌播放器的精简控制层。电视端忽略
 * @param onEnterFullScreen 触摸端：内嵌控制层的「全屏」按钮
 * @param onExitFullScreen 触摸端：全屏控制层的「返回」与「退出全屏」按钮
 * @param onExit 退出播放（电视返回键 / 内嵌控制层返回按钮）。调用前已上报一次心跳
 * @param onSendHeartbeat 上报播放进度（秒；播完为 -1），接 VideoPlayerV3ViewModel.uploadHistory。
 *   每 15 秒（播放中）、暂停、换集、播完、退出时调用；无痕模式（incognitoMode）下不调用
 * @param onClearBackToHistoryData 「回到上次进度」提示结束或已跳转，接 `lastPlayed = 0`
 * @param onLoadNextVideo 当前视频播完，接 VideoPlayerV3ViewModel.playNextVideo（按播放模式处理）
 * @param onLoadNewVideo 切换到选集列表里的某一项
 * @param onResolutionChange / onCodecChange / onAudioChange 切换清晰度 / 编码 / 音质；
 *   处理完后调用 afterChange()，播放器会回到切换前的进度继续播放
 * @param onDanmakuEnabledChange 弹幕总开关（触摸端「弹 开」标签、弹幕面板开关、电视菜单「弹幕」项）
 * @param onDanmakuSwitchChange 显示的弹幕类型（含 DanmakuType.All）
 * @param onDanmakuSizeChange 弹幕字号缩放
 * @param ownerName UP 主名字，电视左上角副标题「{视频标题} · {UP}」用
 * @param onOpenComments 电视控制栏「评论」按钮；为 null 时不显示该按钮
 * @param onEmbeddedMore 触摸端内嵌控制层右上角「更多」按钮；为 null 时不显示
 * @param showRemoteControlHint 电视端：在播放器上方显示遥控器操作提示（是否只显示一次由调用方决定）
 * @param onRemoteControlHintDismiss 遥控器操作提示被关闭
 * @param reduceMotion 「减少动效」：形状变形直接跳到终态
 * @param menuState 菜单状态，见 [PlayerMenuState]
 */
@Composable
fun BvPlayerScreen(
    videoPlayer: AbstractVideoPlayer,
    danmakuPlayer: DanmakuPlayer?,
    isTv: Boolean,
    isTablet: Boolean,
    onExit: () -> Unit,
    onSendHeartbeat: suspend (time: Int) -> Unit,
    onClearBackToHistoryData: () -> Unit,
    onLoadNextVideo: () -> Unit,
    onLoadNewVideo: (VideoListItem) -> Unit,
    onResolutionChange: (Resolution, afterChange: suspend () -> Unit) -> Unit,
    onCodecChange: (VideoCodec, afterChange: suspend () -> Unit) -> Unit,
    onAudioChange: (Audio, afterChange: suspend () -> Unit) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onDanmakuEnabledChange: (Boolean) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = true,
    onEnterFullScreen: () -> Unit = {},
    onExitFullScreen: () -> Unit = {},
    ownerName: String = "",
    onOpenComments: (() -> Unit)? = null,
    onEmbeddedMore: (() -> Unit)? = null,
    showRemoteControlHint: Boolean = false,
    onRemoteControlHintDismiss: () -> Unit = {},
    reduceMotion: Boolean = false,
    menuState: PlayerMenuState = rememberPlayerMenuState(),
) {
    val logger = remember { KotlinLogging.logger("BvPlayerScreen") }
    val scope = rememberCoroutineScope()
    // 心跳不跟随组合的生命周期：退出播放器时最后一次上报不能因为 scope 取消而丢失
    val heartbeatScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    val configData = LocalVideoPlayerConfigData.current
    val latestConfig by rememberUpdatedState(configData)
    val danmakuMaskData = LocalVideoPlayerDanmakuMasksData.current
    val latestDanmakuMasks by rememberUpdatedState(danmakuMaskData.danmakuMasks)
    val historyData = LocalVideoPlayerHistoryData.current
    val loadStateData = LocalVideoPlayerLoadStateData.current
    val videoInfoData = LocalVideoPlayerVideoInfoData.current

    val latestVideoInfo by rememberUpdatedState(videoInfoData)
    val latestIsTv by rememberUpdatedState(isTv)
    val latestOnSendHeartbeat by rememberUpdatedState(onSendHeartbeat)
    val latestOnLoadNextVideo by rememberUpdatedState(onLoadNextVideo)
    val latestOnClearBackToHistoryData by rememberUpdatedState(onClearBackToHistoryData)

    // 直接调用 danmakuPlayer 会始终为 null
    var mDanmakuPlayer: DanmakuPlayer? by remember { mutableStateOf(null) }

    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var exception by remember { mutableStateOf<Exception?>(null) }
    var showBackToHistory by remember { mutableStateOf(false) }

    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPercentage by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var lastPlayed by remember { mutableLongStateOf(0L) }

    var aspectRatioSetting by remember { mutableStateOf(configData.currentVideoAspectRatio) }
    var defaultAspectRatio by remember { mutableFloatStateOf(16 / 9f) }
    val aspectRatio by animateFloatAsState(
        targetValue = aspectRatioSetting.resolveAspectRatio(defaultAspectRatio),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "aspectRatio",
    )

    var hideBackToHistoryTimer: CountDownTimer? by remember { mutableStateOf(null) }
    var currentDanmakuMaskFrame: DanmakuMaskFrame? by remember { mutableStateOf(null) }

    val typeFilter = remember { TypeFilter() }
    var danmakuConfig by remember { mutableStateOf(DanmakuConfig()) }

    // ---------------------------------------------------------------- 弹幕配置

    val applyDanmakuTypeFilter: (List<DanmakuType>, Boolean) -> Unit = { types, enabled ->
        typeFilter.clear()
        val shownTypes = if (enabled) types else emptyList()
        if (!shownTypes.contains(DanmakuType.All)) {
            (DanmakuType.entries - DanmakuType.All - shownTypes.toSet())
                .mapNotNull {
                    when (it) {
                        DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                        DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        else -> null
                    }
                }
                .forEach { typeFilter.addFilterItem(it) }
        }
        logger.info { "Update danmaku type filters: ${typeFilter.filterSet}" }
        danmakuConfig.updateFilter()
        mDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    val applyDanmakuScale: (Float) -> Unit = { scale ->
        danmakuConfig = danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            textSizeScale = scale,
        )
        mDanmakuPlayer?.updateConfig(danmakuConfig)
    }

    val initDanmakuConfig: () -> Unit = {
        val config = latestConfig
        danmakuConfig = danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            textSizeScale = config.currentDanmakuScale,
            dataFilter = listOf(typeFilter)
        )
        applyDanmakuTypeFilter(config.currentDanmakuEnabledList, config.currentDanmakuEnabled)
        logger.info { "Init danmaku config: $danmakuConfig" }
    }

    val applySpeed: (Float) -> Unit = { speed ->
        videoPlayer.speed = speed
        mDanmakuPlayer?.updatePlaySpeed(speed)
    }

    // ---------------------------------------------------------------- 心跳

    /** [fromPlayer] 为 false 时用最近一次轮询到的进度（退出时播放内核可能已被释放）。 */
    val sendHeartbeat: (fromPlayer: Boolean) -> Unit = { fromPlayer ->
        if (!latestConfig.incognitoMode) {
            val position = if (fromPlayer) videoPlayer.currentPosition else currentPosition
            val total = if (fromPlayer) videoPlayer.duration else duration
            val currentTime = (position.coerceAtLeast(0L) / 1000).toInt()
            val totalTime = (total.coerceAtLeast(0L) / 1000).toInt()
            //播放完后上报的时间应为 -1
            val time = if (currentTime >= totalTime) -1 else currentTime
            // 时长未知（还没加载出来）时不上报，避免误报为「已看完」
            if (totalTime > 0) heartbeatScope.launch {
                runCatching { latestOnSendHeartbeat(time) }
                    .onFailure { logger.fInfo { "Send heartbeat failed: ${it.stackTraceToString()}" } }
            }
        }
    }

    // ---------------------------------------------------------------- 回到上次进度

    val updateBackToHistory: () -> Unit = {
        // 此处使用 videoPlayerHistoryData.lastPlayed 无法获取到新值
        if (lastPlayed > 0 && hideBackToHistoryTimer == null) {
            logger.info { "show showBackToHistory: $lastPlayed" }
            showBackToHistory = true
            hideBackToHistoryTimer = countDownTimer(5000, 1000, "hideBackToHistoryTimer") {
                showBackToHistory = false
                hideBackToHistoryTimer = null
                latestOnClearBackToHistoryData()
            }
        }
    }

    val videoPlayerListener = remember {
        object : VideoPlayerListener {
            override fun onError(error: Exception) {
                logger.info { "onError: $error" }
                isError = true
                exception = error.cause as Exception?
            }

            override fun onReady() {
                logger.info { "onReady" }
                isError = false
                exception = null
                initDanmakuConfig()

                if (latestVideoInfo.width <= 0 || latestVideoInfo.height <= 0) {
                    val ratio = videoPlayer.videoWidth / videoPlayer.videoHeight.toFloat()
                    if (ratio > 0 && ratio.isFinite()) defaultAspectRatio = ratio
                }
                // 触摸端旧逻辑：准备好后立即播放；电视端由 setOptions() 的 playWhenReady 控制
                if (!latestIsTv) videoPlayer.start()

                //reset default play speed
                val speed = latestConfig.currentVideoSpeed
                logger.info { "Reset default play speed: $speed" }
                applySpeed(speed)
            }

            override fun onPlay() {
                logger.info { "onPlay" }
                mDanmakuPlayer?.start()
                isPlaying = true
                isBuffering = false
                updateBackToHistory()
            }

            override fun onPause() {
                logger.info { "onPause" }
                mDanmakuPlayer?.pause()
                isPlaying = false
            }

            override fun onBuffering() {
                logger.info { "onBuffering" }
                isBuffering = true
                mDanmakuPlayer?.pause()
            }

            override fun onEnd() {
                logger.info { "onEnd" }
                mDanmakuPlayer?.pause()
                isPlaying = false
                sendHeartbeat(true)
                latestOnLoadNextVideo()
            }

            override fun onIdle() {
                logger.info { "onIdle" }
                mDanmakuPlayer?.pause()
            }

            override fun onSeekBack(seekBackIncrementMs: Long) {
                mDanmakuPlayer?.seekTo(currentPosition)
            }

            override fun onSeekForward(seekForwardIncrementMs: Long) {
                mDanmakuPlayer?.seekTo(currentPosition)
            }
        }
    }

    // ---------------------------------------------------------------- 副作用

    // 播放/缓冲时保持屏幕常亮；暂停、结束、出错后允许熄屏
    val view = LocalView.current
    DisposableEffect(view, isPlaying, isBuffering, isError) {
        view.keepScreenOn = !isError && (isPlaying || isBuffering)
        onDispose { view.keepScreenOn = false }
    }

    // updateBackToHistory() 中使用 videoPlayerHistoryData.lastPlayed 无法获取到新值
    LaunchedEffect(historyData.lastPlayed) {
        lastPlayed = historyData.lastPlayed.toLong()
    }

    LaunchedEffect(videoInfoData.width, videoInfoData.height) {
        val ratio = videoInfoData.width / videoInfoData.height.toFloat()
        defaultAspectRatio = ratio.takeIf { it > 0 && it.isFinite() } ?: (16 / 9f)
    }

    LaunchedEffect(configData.currentVideoAspectRatio) {
        aspectRatioSetting = configData.currentVideoAspectRatio
    }

    LaunchedEffect(danmakuPlayer) {
        logger.debug { "update mDanmakuPlayer" }
        mDanmakuPlayer = danmakuPlayer
    }

    LaunchedEffect(loadStateData.loadState) {
        if (loadStateData.loadState == RequestState.Failed) {
            exception = Exception(loadStateData.errorMessage)
            isError = true
        }
    }

    // 进度轮询
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = videoPlayer.currentPosition.coerceAtLeast(0L)
            duration = videoPlayer.duration.coerceAtLeast(0L)
            bufferedPercentage = videoPlayer.bufferedPercentage
            delay(100)
        }
    }

    // 播放心跳：5 秒后开始，每 15 秒上报一次（播放中）；离开播放器时再上报一次
    LaunchedEffect(Unit) {
        delay(5000)
        while (true) {
            if (videoPlayer.isPlaying) sendHeartbeat(true)
            delay(15000)
        }
    }
    DisposableEffect(Unit) {
        onDispose { sendHeartbeat(false) }
    }

    // 弹幕蒙版（照搬旧电视版）
    DisposableEffect(Unit) {
        var updateMaskTimer: Timer? = null
        var resetTimer: ((Long) -> Unit)? = null

        val updateMask: suspend () -> Unit = {
            val position = currentPosition
            val danmakuMasks = latestDanmakuMasks.firstOrNull {
                position in it.range
            }?.frames?.firstOrNull { position in it.range }
            withContext(Dispatchers.Main) { currentDanmakuMaskFrame = danmakuMasks }

            val frame = danmakuMasks
            if (frame != null) {
                resetTimer?.invoke(max(frame.range.last - position + 3, 20))
            } else {
                resetTimer?.invoke(2000)
            }
        }

        val timerTask: () -> Unit = {
            val position = currentPosition
            scope.launch(Dispatchers.IO) {
                val frame = currentDanmakuMaskFrame
                if (latestDanmakuMasks.isNotEmpty()) {
                    if (frame == null) {
                        //当前无蒙版
                        updateMask()
                    } else if (position !in frame.range) {
                        //当前蒙版过期
                        updateMask()
                    } else {
                        //正常情况下不会在未过期时运行到此代码块，除非是卡顿等情况
                        if (isPlaying) {
                            resetTimer?.invoke(max(frame.range.last - position + 3, 20))
                        } else {
                            resetTimer?.invoke(2000)
                        }
                    }
                } else {
                    //定期检查是否有蒙版
                    withContext(Dispatchers.Main) { currentDanmakuMaskFrame = null }
                    resetTimer?.invoke(2000)
                }
            }
        }

        resetTimer = { delay ->
            updateMaskTimer = timeTask(delay, "updateDanmakuMask", false) {
                timerTask()
            }
        }
        resetTimer.invoke(0)

        onDispose {
            resetTimer = null
            updateMaskTimer?.cancel()
        }
    }

    DisposableEffect(Unit) {
        onDispose { hideBackToHistoryTimer?.cancel() }
    }

    // ---------------------------------------------------------------- 菜单状态

    menuState.config = configData
    menuState.handlers = PlayerMenuHandlers(
        onResolutionChange = { resolution ->
            videoPlayer.pause()
            val current = videoPlayer.currentPosition
            onResolutionChange(resolution) {
                withContext(Dispatchers.Main) {
                    videoPlayer.seekTo(current)
                    videoPlayer.start()
                }
            }
        },
        onCodecChange = { codec ->
            videoPlayer.pause()
            val current = videoPlayer.currentPosition
            onCodecChange(codec) {
                withContext(Dispatchers.Main) {
                    videoPlayer.seekTo(current)
                    videoPlayer.start()
                }
            }
        },
        onAudioChange = { audio ->
            videoPlayer.pause()
            val current = videoPlayer.currentPosition
            onAudioChange(audio) {
                withContext(Dispatchers.Main) {
                    videoPlayer.seekTo(current)
                    videoPlayer.start()
                }
            }
        },
        onPlaySpeedChange = { speed ->
            logger.info { "Set default play speed: $speed" }
            onPlaySpeedChange(speed)
            applySpeed(speed)
        },
        onAspectRatioChange = { ratio ->
            aspectRatioSetting = ratio
            onAspectRatioChange(ratio)
        },
        onDanmakuEnabledChange = { enabled ->
            onDanmakuEnabledChange(enabled)
            applyDanmakuTypeFilter(latestConfig.currentDanmakuEnabledList, enabled)
        },
        onDanmakuTypesChange = { types ->
            logger.info { "On enabled danmaku type change: $types" }
            onDanmakuSwitchChange(types)
            applyDanmakuTypeFilter(types, latestConfig.currentDanmakuEnabled)
        },
        onDanmakuScaleChange = { scale ->
            onDanmakuSizeChange(scale)
            applyDanmakuScale(scale)
        },
        onDanmakuOpacityChange = onDanmakuOpacityChange,
        onDanmakuAreaChange = onDanmakuAreaChange,
        onDanmakuMaskChange = onDanmakuMaskChange,
        onSubtitleChange = onSubtitleChange,
        onSubtitleSizeChange = onSubtitleSizeChange,
        onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
        onSubtitleBottomPaddingChange = onSubtitleBottomPadding,
        onPlayModeChange = onPlayModeChange,
    )

    // ---------------------------------------------------------------- 控制层

    val controller = PlayerController(
        videoPlayer = videoPlayer,
        menuState = menuState,
        play = { videoPlayer.start() },
        pause = {
            videoPlayer.pause()
            sendHeartbeat(true)
        },
        seekTo = { position ->
            videoPlayer.seekTo(position)
            mDanmakuPlayer?.seekTo(position)
            // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
            mDanmakuPlayer?.pause()
        },
        backToHistory = {
            val time = lastPlayed
            logger.fInfo { "Back to history: ${time.formatHourMinSec()}" }
            videoPlayer.seekTo(time)
            mDanmakuPlayer?.seekTo(time)
            // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
            mDanmakuPlayer?.pause()
            onClearBackToHistoryData()
            showBackToHistory = false
            hideBackToHistoryTimer?.cancel()
            hideBackToHistoryTimer = null
        },
        playNewVideo = { item ->
            sendHeartbeat(true)
            onLoadNewVideo(item)
        },
        exit = {
            sendHeartbeat(true)
            onExit()
        },
        setTemporarySpeed = { speed -> applySpeed(speed ?: latestConfig.currentVideoSpeed) },
    )

    val videoSurface: @Composable BoxScope.() -> Unit = {
        if (isTv) {
            LaunchedEffect(Unit) {
                videoPlayer.setOptions()
            }
        }

        // 等宽：宽度铺满、高度按比例；等高：高度铺满、宽度按比例；拉伸：直接铺满整个容器
        val videoModifier = when (aspectRatioSetting) {
            VideoAspectRatio.EqualWidth -> Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)

            VideoAspectRatio.EqualHeight -> Modifier
                .fillMaxHeight()
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)

            VideoAspectRatio.Stretch -> Modifier.fillMaxSize()

            else -> Modifier.aspectRatio(aspectRatio)
        }

        BvVideoPlayer(
            modifier = videoModifier.align(Alignment.Center),
            videoPlayer = videoPlayer,
            playerListener = videoPlayerListener,
        )

        AkDanmakuPlayer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(configData.currentDanmakuArea)
                .alpha(configData.currentDanmakuOpacity)
                .ifElse(
                    { configData.currentDanmakuMask },
                    Modifier.danmakuMask(currentDanmakuMaskFrame)
                ),
            danmakuPlayer = mDanmakuPlayer
        )
    }

    val dpad = rememberDpadMode(isTv)

    CompositionLocalProvider(
        LocalVideoPlayerSeekData provides VideoPlayerSeekData(
            duration = duration,
            position = currentPosition,
            bufferedPercentage = bufferedPercentage
        ),
        LocalVideoPlayerStateData provides VideoPlayerStateData(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            isError = isError,
            exception = exception,
            showBackToHistory = showBackToHistory
        ),
        LocalVideoPlayerDebugInfoData provides VideoPlayerDebugInfoData(
            debugInfo = videoPlayer.debugInfo
        ),
        LocalPlayerDpad provides dpad,
        LocalPlayerReduceMotion provides reduceMotion,
    ) {
        if (isTv) {
            TvController(
                modifier = modifier,
                controller = controller,
                ownerName = ownerName,
                onOpenComments = onOpenComments,
                showRemoteControlHint = showRemoteControlHint,
                onRemoteControlHintDismiss = onRemoteControlHintDismiss,
                content = videoSurface,
            )
        } else {
            TouchController(
                modifier = modifier,
                controller = controller,
                isTablet = isTablet,
                isFullScreen = isFullScreen,
                onEnterFullScreen = onEnterFullScreen,
                onExitFullScreen = onExitFullScreen,
                onEmbeddedMore = onEmbeddedMore,
                content = videoSurface,
            )
        }
    }
}

/** 控制层共用的播放器操作（由 [BvPlayerScreen] 组装，触摸端和电视端共用）。 */
internal class PlayerController(
    val videoPlayer: AbstractVideoPlayer,
    val menuState: PlayerMenuState,
    val play: () -> Unit,
    /** 暂停并上报心跳。 */
    val pause: () -> Unit,
    /** 跳转到指定进度（同时同步弹幕）。 */
    val seekTo: (Long) -> Unit,
    /** 跳回上次播放进度。 */
    val backToHistory: () -> Unit,
    /** 上报心跳后切换到选集里的另一项。 */
    val playNewVideo: (VideoListItem) -> Unit,
    /** 上报心跳后退出播放器。 */
    val exit: () -> Unit,
    /** 临时倍速（触摸端长按 2×、电视端长按确认键用设置值）；null 恢复为当前设置的倍速。不写入设置。 */
    val setTemporarySpeed: (Float?) -> Unit,
)
