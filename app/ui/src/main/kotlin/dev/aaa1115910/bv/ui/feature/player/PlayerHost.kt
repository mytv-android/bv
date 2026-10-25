package dev.aaa1115910.bv.ui.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.aaa1115910.bv.entity.ThemeSource
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.danmaku.DanmakuView
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerInteractiveData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoShotData
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerInteractiveData
import dev.aaa1115910.bv.player.entity.VideoPlayerDanmakuMasksData
import dev.aaa1115910.bv.player.entity.VideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.VideoPlayerLoadStateData
import dev.aaa1115910.bv.player.entity.VideoPlayerLogsData
import dev.aaa1115910.bv.player.entity.VideoPlayerPaymentData
import dev.aaa1115910.bv.player.entity.VideoPlayerSeekThumbData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoPlayerVideoShotData
import dev.aaa1115910.bv.player.ui.BvPlayerScreen
import dev.aaa1115910.bv.ui.designsystem.theme.BvTheme
import dev.aaa1115910.bv.ui.designsystem.theme.CoverSeedCache
import dev.aaa1115910.bv.ui.designsystem.theme.CoverTheme
import dev.aaa1115910.bv.ui.designsystem.theme.LocalThemePrefs
import dev.aaa1115910.bv.ui.feature.verify.GeetestTvVerifyDialog
import dev.aaa1115910.bv.ui.feature.verify.GeetestVerifyDialogHost
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.ui.PreferenceWrites
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 播放器宿主（第 9.4 节）：提供旧播放页提供的全部 `LocalVideoPlayer*Data`，
 * 回调照抄旧电视 VideoPlayerV3Screen（弹幕总开关照抄旧手机 onToggleDanmaku），
 * 外层包 `BvTheme(forceDark = true) { CoverTheme(cover, forceDark = true) { … } }`。
 *
 * 播放内核由调用方创建并传入（[videoPlayer]），离开组合时由 `BvVideoPlayer` 释放；
 * 弹幕引擎 View 由调用方创建并传入（[danmakuView]），离开组合时释放。
 *
 * @param onLoadNewVideo 选集面板切换分 P / 剧集；默认照抄旧电视版直接让 ViewModel 播放
 */
@Composable
internal fun BvPlayerHost(
    playerVm: VideoPlayerV3ViewModel,
    videoPlayer: AbstractVideoPlayer,
    danmakuView: DanmakuView?,
    cover: String?,
    isTv: Boolean,
    isTablet: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = true,
    onEnterFullScreen: () -> Unit = {},
    onExitFullScreen: () -> Unit = {},
    ownerName: String = "",
    onOpenComments: (() -> Unit)? = null,
    onEmbeddedMore: (() -> Unit)? = null,
    showRemoteControlHint: Boolean = false,
    onRemoteControlHintDismiss: () -> Unit = {},
    onLoadNewVideo: ((VideoListItemData) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePrefs = LocalThemePrefs.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnLoadNewVideo by rememberUpdatedState(onLoadNewVideo)

    // Prefs 读取是 runBlocking 的 DataStore 访问：只在进入播放页时读一次，
    // 避免播放状态每次变化都阻塞主线程
    val incognitoMode = remember { Prefs.incognitoMode }
    val playerLongPressAction = remember { Prefs.playerLongPressAction }
    val playerLongPressSpeed = remember { Prefs.playerLongPressSpeed }
    val skipPgcIntroOutro = remember { Prefs.skipPgcIntroOutro }

    // 旧播放页 onPause：暂停视频（弹幕引擎通过 isPlayingProvider 跟随，无需单独暂停）
    DisposableEffect(lifecycleOwner, videoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                videoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 第 6.5 节「跟随内容」：开始播放一个视频时用它的封面色作为全局种子色
    LaunchedEffect(cover, themePrefs.source) {
        if (themePrefs.source != ThemeSource.Content || cover.isNullOrEmpty()) return@LaunchedEffect
        val seed = CoverSeedCache.get(context, cover) ?: return@LaunchedEffect
        Prefs.setContentSeed(seed)
    }

    BvTheme(forceDark = true) {
        CoverTheme(cover, forceDark = true) {
            CompositionLocalProvider(
                LocalVideoPlayerSeekThumbData provides VideoPlayerSeekThumbData(
                    idleIcon = playerVm.playerIconIdle,
                    movingIcon = playerVm.playerIconMoving
                ),
                LocalVideoPlayerVideoInfoData provides VideoPlayerVideoInfoData(
                    width = playerVm.currentVideoWidth,
                    height = playerVm.currentVideoHeight,
                    codec = playerVm.currentVideoCodec.name,
                    title = playerVm.title,
                    partTitle = playerVm.partTitle,
                ),
                LocalVideoPlayerLogsData provides VideoPlayerLogsData(
                    logs = playerVm.logs
                ),
                LocalVideoPlayerHistoryData provides VideoPlayerHistoryData(
                    lastPlayed = playerVm.lastPlayed,
                ),
                LocalVideoPlayerPaymentData provides VideoPlayerPaymentData(
                    needPay = playerVm.needPay,
                    epid = playerVm.epid,
                    isPreviewPlayback = playerVm.isPreviewPlayback,
                    showPreviewTip = playerVm.showPreviewTip,
                ),
                LocalVideoPlayerInteractiveData provides VideoPlayerInteractiveData(
                    isInteractive = playerVm.isInteractivePlayback,
                    showDialog = playerVm.showInteractiveOptionDialog,
                    options = playerVm.interactiveOptions,
                ),
                LocalVideoPlayerLoadStateData provides VideoPlayerLoadStateData(
                    loadState = playerVm.loadState,
                    errorMessage = playerVm.errorMessage,
                ),
                LocalVideoPlayerConfigData provides VideoPlayerConfigData(
                    availableResolutions = playerVm.availableQuality,
                    availableVideoCodec = playerVm.availableVideoCodec,
                    availableAudio = playerVm.availableAudio,
                    availableSubtitleTracks = playerVm.availableSubtitle,
                    availableVideoList = playerVm.availableVideoList,
                    currentVideoCid = playerVm.currentCid,
                    currentResolution = playerVm.currentQuality,
                    currentVideoCodec = playerVm.currentVideoCodec,
                    currentVideoAspectRatio = playerVm.currentVideoAspectRatio,
                    currentVideoSpeed = playerVm.currentPlaySpeed,
                    currentAudio = playerVm.currentAudio,
                    currentDanmakuEnabled = playerVm.currentDanmakuEnabled,
                    currentDanmakuEnabledList = playerVm.currentDanmakuTypes,
                    currentDanmakuScale = playerVm.currentDanmakuScale,
                    currentDanmakuOpacity = playerVm.currentDanmakuOpacity,
                    currentDanmakuArea = playerVm.currentDanmakuArea,
                    currentDanmakuMask = playerVm.currentDanmakuMask,
                    currentDanmakuLaneDensity = playerVm.currentDanmakuLaneDensity,
                    currentDanmakuRollingDurationFactor = playerVm.currentDanmakuRollingDurationFactor,
                    currentDanmakuFilterLevel = playerVm.currentDanmakuFilterLevel,
                    currentSubtitleId = playerVm.currentSubtitleId,
                    currentSubtitleData = playerVm.currentSubtitleData,
                    currentSubtitleFontSize = playerVm.currentSubtitleFontSize,
                    currentSubtitleBackgroundOpacity = playerVm.currentSubtitleBackgroundOpacity,
                    currentSubtitleBottomPadding = playerVm.currentSubtitleBottomPadding,
                    currentPlayMode = playerVm.currentPlayMode,
                    incognitoMode = incognitoMode,
                    longPressAction = playerLongPressAction,
                    longPressSpeed = playerLongPressSpeed,
                    clipInfoList = playerVm.clipInfoList,
                    skipPgcIntroOutro = skipPgcIntroOutro,
                ),
                LocalVideoPlayerDanmakuMasksData provides VideoPlayerDanmakuMasksData(
                    danmakuMasks = playerVm.danmakuMasks,
                ),
                LocalVideoPlayerVideoShotData provides VideoPlayerVideoShotData(
                    videoShot = playerVm.videoShot,
                ),
            ) {
                // 风控验证弹窗：电视端用遥控器滑块对话框，触屏端用极验 SDK 对话框
                if (isTv) {
                    if (playerVm.showGeetestDialog) {
                        GeetestTvVerifyDialog(
                            gt = playerVm.geetestGt,
                            challenge = playerVm.geetestChallenge,
                            onResult = { result ->
                                playerVm.onGeetestResult(
                                    result.challenge,
                                    result.validate,
                                    result.seccode
                                )
                            },
                            onDismiss = playerVm::onGeetestCancelled,
                        )
                    }
                } else {
                    GeetestVerifyDialogHost(
                        show = playerVm.showGeetestDialog,
                        gt = playerVm.geetestGt,
                        challenge = playerVm.geetestChallenge,
                        onResult = playerVm::onGeetestResult,
                        onCancelled = playerVm::onGeetestCancelled,
                    )
                }
                BvPlayerScreen(
                    videoPlayer = videoPlayer,
                    danmakuView = danmakuView,
                    isTv = isTv,
                    isTablet = isTablet,
                    onExit = onExit,
                    onSendHeartbeat = { time -> playerVm.uploadHistory(time) },
                    onClearBackToHistoryData = { playerVm.lastPlayed = 0 },
                    onLoadNextVideo = { playerVm.playNextVideo() },
                    onLoadNewVideo = { videoListItem ->
                        if (videoListItem is VideoListItemData) {
                            if (videoListItem is VideoListInteractiveNode) {
                                // 互动分支：走状态机切换（记录节点路径、跳到节点起始位置），
                                // 优先于页面自己的 override，避免重建选集列表破坏节点图
                                playerVm.playInteractiveOption(videoListItem)
                            } else {
                                val override = latestOnLoadNewVideo
                                if (override != null) {
                                    override(videoListItem)
                                } else {
                                    playerVm.partTitle = videoListItem.title
                                    playerVm.loadPlayUrl(
                                        avid = videoListItem.aid,
                                        cid = videoListItem.cid,
                                        epid = videoListItem.epid,
                                        seasonId = videoListItem.seasonId,
                                        continuePlayNext = true
                                    )
                                }
                            }
                        }
                    },
                    onResolutionChange = { resolution, afterChange ->
                        scope.launch(Dispatchers.Default) {
                            playerVm.playQuality(resolution)
                            afterChange()
                            withContext(Dispatchers.Main) { playerVm.currentQuality = resolution }
                        }
                    },
                    onCodecChange = { videoCodec, afterChange ->
                        playerVm.currentVideoCodec = videoCodec
                        scope.launch(Dispatchers.Default) {
                            playerVm.playQuality(playerVm.currentQuality, playerVm.currentVideoCodec)
                            afterChange()
                        }
                    },
                    onAudioChange = { audio, afterChange ->
                        playerVm.currentAudio = audio
                        scope.launch(Dispatchers.Default) {
                            playerVm.playQuality(audio = audio)
                            afterChange()
                        }
                    },
                    onAspectRatioChange = { aspectRatio ->
                        playerVm.currentVideoAspectRatio = aspectRatio
                    },
                    onPlaySpeedChange = { speed ->
                        PreferenceWrites.enqueue { Prefs.defaultPlaySpeed = speed }
                        playerVm.currentPlaySpeed = speed
                    },
                    onDanmakuEnabledChange = { enabled ->
                        playerVm.currentDanmakuEnabled = enabled
                        PreferenceWrites.enqueue { Prefs.defaultDanmakuEnabled = enabled }
                    },
                    onDanmakuSwitchChange = { enabledDanmakuTypes ->
                        val savedTypes = enabledDanmakuTypes.toList()
                        PreferenceWrites.enqueue { Prefs.defaultDanmakuTypes = savedTypes }
                        playerVm.currentDanmakuTypes.clear()
                        playerVm.currentDanmakuTypes.addAll(enabledDanmakuTypes)
                    },
                    onDanmakuSizeChange = { scale ->
                        PreferenceWrites.enqueueLatest("danmaku-scale") { Prefs.defaultDanmakuScale = scale }
                        playerVm.currentDanmakuScale = scale
                    },
                    onDanmakuOpacityChange = { opacity ->
                        PreferenceWrites.enqueueLatest("danmaku-opacity") { Prefs.defaultDanmakuOpacity = opacity }
                        playerVm.currentDanmakuOpacity = opacity
                    },
                    onDanmakuAreaChange = { area ->
                        PreferenceWrites.enqueueLatest("danmaku-area") { Prefs.defaultDanmakuArea = area }
                        playerVm.currentDanmakuArea = area
                    },
                    onDanmakuMaskChange = { mask ->
                        PreferenceWrites.enqueue { Prefs.defaultDanmakuMask = mask }
                        playerVm.currentDanmakuMask = mask
                    },
                    onDanmakuLaneDensityChange = { density ->
                        PreferenceWrites.enqueue { Prefs.defaultDanmakuLaneDensity = density }
                        playerVm.currentDanmakuLaneDensity = density
                    },
                    onDanmakuFilterLevelChange = { level ->
                        PreferenceWrites.enqueueLatest("danmaku-filter-level") { Prefs.defaultDanmakuFilterLevel = level }
                        playerVm.currentDanmakuFilterLevel = level
                    },
                    onSubtitleChange = { subtitle ->
                        playerVm.loadSubtitle(subtitle.id)
                    },
                    onSubtitleSizeChange = { size ->
                        PreferenceWrites.enqueueLatest("subtitle-size") { Prefs.defaultSubtitleFontSize = size }
                        playerVm.currentSubtitleFontSize = size
                    },
                    onSubtitleBackgroundOpacityChange = { opacity ->
                        PreferenceWrites.enqueueLatest("subtitle-background") { Prefs.defaultSubtitleBackgroundOpacity = opacity }
                        playerVm.currentSubtitleBackgroundOpacity = opacity
                    },
                    onSubtitleBottomPadding = { padding ->
                        PreferenceWrites.enqueueLatest("subtitle-bottom") { Prefs.defaultSubtitleBottomPadding = padding }
                        playerVm.currentSubtitleBottomPadding = padding
                    },
                    onPlayModeChange = { playMode ->
                        PreferenceWrites.enqueue { Prefs.defaultPlayMode = playMode }
                        playerVm.currentPlayMode = playMode
                    },
                    modifier = modifier,
                    isFullScreen = isFullScreen,
                    onEnterFullScreen = onEnterFullScreen,
                    onExitFullScreen = onExitFullScreen,
                    ownerName = ownerName,
                    onOpenComments = onOpenComments,
                    onEmbeddedMore = onEmbeddedMore,
                    showRemoteControlHint = showRemoteControlHint,
                    onRemoteControlHintDismiss = onRemoteControlHintDismiss,
                    reduceMotion = themePrefs.reduceMotion,
                )
                // 互动视频选项弹窗：节点播完弹出（Dialog 独立窗口，浮在播放器上）
                val interactiveData = LocalVideoPlayerInteractiveData.current
                if (interactiveData.isInteractive &&
                    interactiveData.showDialog &&
                    interactiveData.options.isNotEmpty()
                ) {
                    InteractiveOptionDialog(
                        options = interactiveData.options,
                        currentCid = playerVm.currentCid,
                        onSelectOption = playerVm::playInteractiveOption,
                        onDismiss = playerVm::dismissInteractiveOptionDialog,
                        onExit = onExit,
                    )
                }
            }
        }
    }
}
