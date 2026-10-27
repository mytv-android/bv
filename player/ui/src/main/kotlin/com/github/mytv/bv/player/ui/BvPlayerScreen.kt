@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package com.github.mytv.bv.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.http.entity.video.ClipType
import com.github.mytv.bv.player.AbstractVideoPlayer
import com.github.mytv.bv.player.BvDanmakuPlayer
import com.github.mytv.bv.player.BvVideoPlayer
import com.github.mytv.bv.player.VideoPlayerListener
import com.github.mytv.bv.player.danmaku.DanmakuConfig
import com.github.mytv.bv.player.danmaku.DanmakuLaneDensity
import com.github.mytv.bv.player.danmaku.DanmakuView
import com.github.mytv.bv.player.entity.Audio
import com.github.mytv.bv.player.entity.DanmakuType
import com.github.mytv.bv.player.entity.LocalVideoPlayerConfigData
import com.github.mytv.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import com.github.mytv.bv.player.entity.LocalVideoPlayerDebugInfoData
import com.github.mytv.bv.player.entity.LocalVideoPlayerHistoryData
import com.github.mytv.bv.player.entity.LocalVideoPlayerLoadStateData
import com.github.mytv.bv.player.entity.LocalVideoPlayerSeekData
import com.github.mytv.bv.player.entity.LocalVideoPlayerStateData
import com.github.mytv.bv.player.entity.LocalVideoPlayerVideoInfoData
import com.github.mytv.bv.player.entity.PlayMode
import com.github.mytv.bv.player.entity.RequestState
import com.github.mytv.bv.player.entity.Resolution
import com.github.mytv.bv.player.entity.VideoAspectRatio
import com.github.mytv.bv.player.entity.VideoCodec
import com.github.mytv.bv.player.entity.VideoListItem
import com.github.mytv.bv.player.entity.VideoPlayerDebugInfoData
import com.github.mytv.bv.player.entity.VideoPlayerSeekData
import com.github.mytv.bv.player.entity.VideoPlayerStateData
import com.github.mytv.bv.player.ui.design.LocalPlayerDpad
import com.github.mytv.bv.player.ui.design.LocalPlayerFocusColor
import com.github.mytv.bv.player.ui.design.LocalPlayerReduceMotion
import com.github.mytv.bv.player.ui.design.rememberDpadMode
import com.github.mytv.bv.player.ui.touch.TouchController
import com.github.mytv.bv.player.ui.touch.VideoZoomState
import com.github.mytv.bv.player.ui.tv.TvController
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.formatHourMinSec
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 统一播放器入口（第 11 节）：合并旧 `player/mobile` 与 `player/tv` 的 `BvPlayer`。
 * 内部 `if (isTv) TvController(...) else TouchController(...)`。
 *
 * 本组件**不安装主题**：调用方负责在外层包 `BvTheme(forceDark = true) { CoverTheme(cover, forceDark = true) { … } }`。
 * 和旧版一样，播放数据通过 `LocalVideoPlayer*Data` 系列 CompositionLocal 由调用方提供
 * （VideoInfo / Logs / History / Payment / LoadState / Config / DanmakuMasks / VideoShot / SeekThumb）。
 *
 * @param videoPlayer 播放内核（VideoPlayerV3ViewModel.videoPlayer）
 * @param danmakuView 弹幕引擎渲染 View（VideoPlayerV3ViewModel.danmakuView，由调用方创建并释放）
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
    danmakuView: DanmakuView?,
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
    onDanmakuLaneDensityChange: (DanmakuLaneDensity) -> Unit,
    onDanmakuFilterLevelChange: (Int) -> Unit,
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
    // 心跳不跟随组合的生命周期：退出播放器时最后一次上报不能因为 scope 取消而丢失
    val heartbeatScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    val configData = LocalVideoPlayerConfigData.current
    val latestConfig by rememberUpdatedState(configData)
    val danmakuMaskData = LocalVideoPlayerDanmakuMasksData.current
    val latestDanmakuMasks by rememberUpdatedState(danmakuMaskData.danmakuMasks)
    val historyData = LocalVideoPlayerHistoryData.current
    val loadStateData = LocalVideoPlayerLoadStateData.current
    val videoInfoData = LocalVideoPlayerVideoInfoData.current

    // 双指缩放只用于触摸端全屏：SurfaceView 放大会超出内嵌容器
    val videoZoomState = remember { VideoZoomState() }
    val zoomEnabled = !isTv && isFullScreen

    val latestVideoInfo by rememberUpdatedState(videoInfoData)
    val latestIsTv by rememberUpdatedState(isTv)
    val latestOnSendHeartbeat by rememberUpdatedState(onSendHeartbeat)
    val latestOnLoadNextVideo by rememberUpdatedState(onLoadNextVideo)
    val latestOnClearBackToHistoryData by rememberUpdatedState(onClearBackToHistoryData)

    // 长期监听器始终读取最新 View，不再通过协程复制成另一份状态。
    val mDanmakuView by rememberUpdatedState(danmakuView)

    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var exception by remember { mutableStateOf<Exception?>(null) }
    var showBackToHistory by remember { mutableStateOf(false) }

    // 轮询结果只给心跳/弹幕回调使用，不放进 Composition，避免 10Hz 重组整屏
    val progressSnapshot = remember { PlayerProgressSnapshot() }
    var lastPlayed by remember { mutableLongStateOf(0L) }

    var aspectRatioSetting by remember { mutableStateOf(configData.currentVideoAspectRatio) }
    var defaultAspectRatio by remember { mutableFloatStateOf(16 / 9f) }
    val aspectRatio by animateFloatAsState(
        targetValue = aspectRatioSetting.resolveAspectRatio(defaultAspectRatio),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "aspectRatio",
    )

    // 换集、切画幅比例、全屏状态变化后回到 1x
    KeyedSideEffect(configData.currentVideoCid, aspectRatioSetting, isFullScreen) {
        videoZoomState.reset()
    }

    if (showBackToHistory) {
        LaunchedEffect(Unit) {
            delay(5000)
            // Clear locally before a player callback can re-show the expired tip.
            lastPlayed = 0L
            showBackToHistory = false
            latestOnClearBackToHistoryData()
        }
    }

    // 跳过 PGC 片头片尾：同一集每个区间只自动跳一次，换集或拿到新配置时重置
    var processedClipIndices by remember { mutableStateOf(emptySet<Int>()) }
    KeyedSideEffect(configData.currentVideoCid, configData.clipInfoList) {
        processedClipIndices = emptySet()
    }

    var danmakuConfig by remember { mutableStateOf(DanmakuConfig()) }

    // ---------------------------------------------------------------- 弹幕配置

    // 类型开关（含总开关）映射为引擎的 allowScroll / allowTop / allowBottom；
    // 被禁用的固定弹幕由引擎转为滚动（或丢弃），语义与旧 TypeFilter 一致。
    val applyDanmakuTypeFilter: (List<DanmakuType>, Boolean) -> Unit = { types, enabled ->
        val allowAll = types.contains(DanmakuType.All)
        danmakuConfig = danmakuConfig.copy(
            enabled = enabled,
            allowScroll = allowAll || types.contains(DanmakuType.Rolling),
            allowTop = allowAll || types.contains(DanmakuType.Top),
            allowBottom = allowAll || types.contains(DanmakuType.Bottom),
        )
        logger.info { "Update danmaku config: $danmakuConfig" }
        mDanmakuView?.setConfig(danmakuConfig)
    }

    val applyDanmakuScale: (Float) -> Unit = { scale ->
        danmakuConfig = danmakuConfig.copy(
            textSizeScale = (scale * 100).toInt(),
        )
        mDanmakuView?.setConfig(danmakuConfig)
    }

    val initDanmakuConfig: () -> Unit = {
        val config = latestConfig
        danmakuConfig = DanmakuConfig(
            enabled = config.currentDanmakuEnabled,
            opacity = config.currentDanmakuOpacity,
            area = config.currentDanmakuArea,
            laneDensity = config.currentDanmakuLaneDensity,
            durationMultiplier = config.currentDanmakuRollingDurationFactor,
            minLevel = config.currentDanmakuFilterLevel,
        )
        applyDanmakuScale(config.currentDanmakuScale)
        applyDanmakuTypeFilter(config.currentDanmakuEnabledList, config.currentDanmakuEnabled)
        logger.info { "Init danmaku config: $danmakuConfig" }
    }

    val applySpeed: (Float) -> Unit = { speed ->
        videoPlayer.speed = speed
    }

    // ---------------------------------------------------------------- 心跳

    /** [fromPlayer] 为 false 时用最近一次轮询到的进度（退出时播放内核可能已被释放）。 */
    val sendHeartbeat: (fromPlayer: Boolean) -> Unit = { fromPlayer ->
        if (!latestConfig.incognitoMode) {
            val position = if (fromPlayer) videoPlayer.currentPosition else progressSnapshot.position
            val total = if (fromPlayer) videoPlayer.duration else progressSnapshot.duration
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
        if (lastPlayed > 0 && !showBackToHistory) {
            logger.info { "show showBackToHistory: $lastPlayed" }
            showBackToHistory = true
        }
    }

    val videoPlayerListener = remember(videoPlayer) {
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
                mDanmakuView?.play()
                isPlaying = true
                isBuffering = false
                updateBackToHistory()
            }

            override fun onPause() {
                logger.info { "onPause" }
                // 弹幕引擎通过 isPlayingProvider 跟随播放器，无需显式暂停
                isPlaying = false
            }

            override fun onBuffering() {
                logger.info { "onBuffering" }
                isBuffering = true
            }

            override fun onEnd() {
                logger.info { "onEnd" }
                isPlaying = false
                sendHeartbeat(true)
                latestOnLoadNextVideo()
            }

            override fun onIdle() {
                logger.info { "onIdle" }
            }

            override fun onSeekBack(seekBackIncrementMs: Long) {
                mDanmakuView?.notifySeek(progressSnapshot.position)
            }

            override fun onSeekForward(seekForwardIncrementMs: Long) {
                mDanmakuView?.notifySeek(progressSnapshot.position)
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
    KeyedSideEffect(historyData.lastPlayed) {
        lastPlayed = historyData.lastPlayed.toLong()
    }

    KeyedSideEffect(videoInfoData.width, videoInfoData.height) {
        val ratio = videoInfoData.width / videoInfoData.height.toFloat()
        defaultAspectRatio = ratio.takeIf { it > 0 && it.isFinite() } ?: (16 / 9f)
    }

    KeyedSideEffect(configData.currentVideoAspectRatio) {
        aspectRatioSetting = configData.currentVideoAspectRatio
    }

    KeyedSideEffect(danmakuView) {
        // View 实例可能晚于配置就绪（或换页面重建），挂上后立即同步一次。
        danmakuView?.setConfig(danmakuConfig)
    }

    KeyedSideEffect(loadStateData.loadState, loadStateData.errorMessage) {
        if (loadStateData.loadState == RequestState.Failed) {
            exception = Exception(loadStateData.errorMessage)
            isError = true
        }
    }

    // 进度轮询 + 跳过 PGC 片头/片尾。
    // 轮询结果写进普通对象（不写 Compose State），界面进度由 PlayerSeekDataProvider 单独提供。
    LaunchedEffect(videoPlayer) {
        while (true) {
            val position = videoPlayer.currentPosition.coerceAtLeast(0L)
            progressSnapshot.position = position
            progressSnapshot.duration = videoPlayer.duration.coerceAtLeast(0L)

            val config = latestConfig
            val clips = config.clipInfoList
            if (config.skipPgcIntroOutro && videoPlayer.isPlaying && !clips.isNullOrEmpty()) {
                val positionSeconds = (position / 1000).toInt()
                val matched = clips.withIndex().firstOrNull { (index, clip) ->
                    index !in processedClipIndices &&
                            clip.isSkippable() &&
                            positionSeconds >= clip.start && positionSeconds < clip.end
                }
                if (matched != null) {
                    val clip = matched.value
                    processedClipIndices = processedClipIndices + matched.index
                    val target = clip.end * 1000L
                    logger.info { "Skip PGC clip: ${clip.clipType}, $positionSeconds -> ${clip.end}" }
                    videoPlayer.seekTo(target)
                    mDanmakuView?.notifySeek(target)
                }
            }

            delay(100)
        }
    }

    // 播放心跳：5 秒后开始，每 15 秒上报一次（播放中）；离开播放器时再上报一次
    LaunchedEffect(videoPlayer) {
        delay(5000)
        while (true) {
            if (videoPlayer.isPlaying) sendHeartbeat(true)
            delay(15000)
        }
    }
    DisposableEffect(videoPlayer) {
        onDispose { sendHeartbeat(false) }
    }

    // 弹幕蒙版：蒙版帧的查找与 DstIn 混合由 DanmakuView 内部完成，这里只同步开关 / 数据 / 画面比例
    KeyedSideEffect(configData.currentDanmakuMask, mDanmakuView) {
        mDanmakuView?.setMaskEnabled(configData.currentDanmakuMask)
    }

    LaunchedEffect(mDanmakuView) {
        snapshotFlow { latestDanmakuMasks.toList() }
            .collect { mDanmakuView?.setMaskSegments(it) }
    }

    LaunchedEffect(mDanmakuView, aspectRatioSetting) {
        val target = mDanmakuView ?: return@LaunchedEffect
        target.setVideoAspectRatioType(aspectRatioSetting)
        snapshotFlow { aspectRatio }.collect { target.setVideoAspectRatio(it) }
    }

    KeyedSideEffect(danmakuView, videoPlayer) {
        danmakuView?.apply {
            setPositionProvider { videoPlayer.currentPosition.coerceAtLeast(0L) }
            setIsPlayingProvider { videoPlayer.isPlaying }
            setPlaybackSpeedProvider { videoPlayer.speed }
        }
    }

    // ---------------------------------------------------------------- 菜单状态

    val menuHandlers = PlayerMenuHandlers(
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
        onDanmakuOpacityChange = { opacity ->
            onDanmakuOpacityChange(opacity)
            danmakuConfig = danmakuConfig.copy(opacity = opacity)
            mDanmakuView?.setConfig(danmakuConfig)
        },
        onDanmakuAreaChange = { area ->
            onDanmakuAreaChange(area)
            danmakuConfig = danmakuConfig.copy(area = area)
            mDanmakuView?.setConfig(danmakuConfig)
        },
        onDanmakuMaskChange = onDanmakuMaskChange,
        onDanmakuLaneDensityChange = { density ->
            onDanmakuLaneDensityChange(density)
            danmakuConfig = danmakuConfig.copy(laneDensity = density)
            mDanmakuView?.setConfig(danmakuConfig)
        },
        onDanmakuFilterLevelChange = { level ->
            onDanmakuFilterLevelChange(level)
            danmakuConfig = danmakuConfig.copy(minLevel = level)
            mDanmakuView?.setConfig(danmakuConfig)
        },
        onSubtitleChange = onSubtitleChange,
        onSubtitleSizeChange = onSubtitleSizeChange,
        onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
        onSubtitleBottomPaddingChange = onSubtitleBottomPadding,
        onPlayModeChange = onPlayModeChange,
    )
    SideEffect {
        menuState.config = configData
        menuState.handlers = menuHandlers
    }

    // ---------------------------------------------------------------- 控制层

    // 用户手动 seek 到片头/片尾区间内时不强行跳走：把目标区间标记为已处理
    val markManualSeekClip: (position: Long) -> Unit = { position ->
        val positionSeconds = (position.coerceAtLeast(0L) / 1000).toInt()
        val handled = configData.clipInfoList.orEmpty().mapIndexedNotNull { index, clip ->
            if (clip.isSkippable() &&
                positionSeconds >= clip.start && positionSeconds < clip.end
            ) index else null
        }
        if (handled.isNotEmpty()) {
            processedClipIndices = processedClipIndices + handled
        }
    }

    val controller = PlayerController(
        videoPlayer = videoPlayer,
        menuState = menuState,
        play = { videoPlayer.start() },
        pause = {
            videoPlayer.pause()
            sendHeartbeat(true)
        },
        seekTo = { position ->
            markManualSeekClip(position)
            videoPlayer.seekTo(position)
            mDanmakuView?.notifySeek(position)
        },
        backToHistory = {
            val time = lastPlayed
            logger.fInfo { "Back to history: ${time.formatHourMinSec()}" }
            markManualSeekClip(time)
            videoPlayer.seekTo(time)
            mDanmakuView?.notifySeek(time)
            lastPlayed = 0L
            showBackToHistory = false
            onClearBackToHistoryData()
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
            // 等 AndroidView 和监听器完成挂载后再设置自动播放。
            LaunchedEffect(videoPlayer) {
                videoPlayer.setOptions()
            }
        }

        // 保持唯一调用点，切换全屏时不能销毁并释放播放内核。
        // 画幅动画和缩放只在测量阶段读取，平移只在放置阶段读取。
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            BvVideoPlayer(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val baseSize = resolveVideoBaseSize(
                            maxWidth = constraints.maxWidth.toFloat(),
                            maxHeight = constraints.maxHeight.toFloat(),
                            aspectRatio = aspectRatio,
                            setting = aspectRatioSetting,
                        )
                        // 普通字段，仅供手势回调使用，不向 Composition 回写快照状态。
                        videoZoomState.contentSize = IntSize(
                            baseSize.width.roundToInt(),
                            baseSize.height.roundToInt(),
                        )
                        val scale = if (zoomEnabled) videoZoomState.scale else 1f
                        val width = (baseSize.width * scale).roundToInt().coerceAtLeast(1)
                        val height = (baseSize.height * scale).roundToInt().coerceAtLeast(1)
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(width, height) { placeable.place(0, 0) }
                    }
                    .offset {
                        val offset = if (zoomEnabled) videoZoomState.offset else Offset.Zero
                        IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                    },
                videoPlayer = videoPlayer,
                playerListener = videoPlayerListener,
            )
        }

        // 弹幕层：显示区域 / 透明度 / 蒙版都由引擎 config 内部处理，View 铺满视频区域即可
        BvDanmakuPlayer(
            modifier = Modifier
                .align(Alignment.Center)
                .matchParentSize(),
            danmakuView = mDanmakuView,
        )
    }

    val dpad = rememberDpadMode(isTv)

    CompositionLocalProvider(
        LocalPlayerFocusColor provides MaterialTheme.colorScheme.primary,
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
        PlayerSeekDataProvider(videoPlayer) {
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
                    zoomState = if (zoomEnabled) videoZoomState else null,
                )
            }
        }
    }
}

/** 进度轮询结果，供心跳、弹幕等回调读取；不参与 Compose 快照。 */
private class PlayerProgressSnapshot {
    @Volatile
    var position: Long = 0L

    @Volatile
    var duration: Long = 0L
}

/**
 * 播放进度只在这个子树里更新：100ms 一次的进度变化只会重组读取
 * [LocalVideoPlayerSeekData] 的控制层，不再让整个播放页跟着重组。
 */
@Composable
private fun PlayerSeekDataProvider(
    videoPlayer: AbstractVideoPlayer,
    content: @Composable () -> Unit,
) {
    val seekData = remember { VideoPlayerSeekData() }
    LaunchedEffect(videoPlayer, seekData) {
        while (true) {
            seekData.position = videoPlayer.currentPosition.coerceAtLeast(0L)
            seekData.duration = videoPlayer.duration.coerceAtLeast(0L)
            seekData.bufferedPercentage = videoPlayer.bufferedPercentage
            delay(100)
        }
    }
    CompositionLocalProvider(
        LocalVideoPlayerSeekData provides seekData,
        content = content,
    )
}

/** 只处理片头/片尾，忽略 HE、多视图、广告等其他 Clip 类型；时间区间无效时也不处理。 */
private fun ClipInfo.isSkippable(): Boolean =
    (clipType == ClipType.Op || clipType == ClipType.Ed) && end > start

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

/** 在测量阶段计算视频基础尺寸，等宽 / 等高模式允许画面超出容器。 */
private fun resolveVideoBaseSize(
    maxWidth: Float,
    maxHeight: Float,
    aspectRatio: Float,
    setting: VideoAspectRatio,
): Size {
    val ratio = aspectRatio.takeIf { it > 0f && it.isFinite() } ?: (16f / 9f)
    return when (setting) {
        VideoAspectRatio.EqualWidth -> Size(maxWidth, maxWidth / ratio)
        VideoAspectRatio.EqualHeight -> Size(maxHeight * ratio, maxHeight)
        VideoAspectRatio.Stretch -> Size(maxWidth, maxHeight)
        else -> if (maxWidth / maxHeight > ratio) {
            Size(maxHeight * ratio, maxHeight)
        } else {
            Size(maxWidth, maxWidth / ratio)
        }
    }
}
