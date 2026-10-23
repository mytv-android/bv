package dev.aaa1115910.bv.player.entity

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.player.danmaku.DanmakuLaneDensity

/**
 * 播放进度以可变 State 持有：CompositionLocal 的值保持不变，
 * 只有真正读取对应字段的组件才会在进度更新时重组。
 */
@Stable
class VideoPlayerSeekData {
    var duration by mutableLongStateOf(1L)
    var position by mutableLongStateOf(0L)
    var bufferedPercentage by mutableIntStateOf(0)
}

data class VideoPlayerSeekThumbData(
    val idleIcon: String = "",
    val movingIcon: String = "",
)

data class VideoPlayerVideoInfoData(
    val width: Int = 0,
    val height: Int = 0,
    val codec: String = "",
    val title: String = "Title",
    val partTitle: String = "PartTitle",
)

data class VideoPlayerClockData(
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
)

data class VideoPlayerLogsData(
    val logs: String = "",
)

data class VideoPlayerHistoryData(
    val lastPlayed: Int = 0,
    val showBackToHistory: Boolean = false,
)

data class VideoPlayerPaymentData(
    val needPay: Boolean = false,
    val epid: Int = 0,
    /** 正在播放可试看的付费内容 */
    val isPreviewPlayback: Boolean = false,
    /** 起播后短暂显示「试看」提示 */
    val showPreviewTip: Boolean = false,
)

/** 互动视频的选项数据（VideoPlayerV3ViewModel 的互动状态机透出）。 */
data class VideoPlayerInteractiveData(
    /** 当前在播互动视频 */
    val isInteractive: Boolean = false,
    /** 播完当前节点后显示选项对话框 */
    val showDialog: Boolean = false,
    /** 当前节点的选项（待选分支，或已走过的节点路径） */
    val options: List<VideoListInteractiveNode> = emptyList(),
)

data class VideoPlayerLoadStateData(
    val loadState: RequestState = RequestState.Ready,
    val errorMessage: String = "",
)

data class VideoPlayerStateData(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isError: Boolean = false,
    val exception: Exception? = null,
    val showBackToHistory: Boolean = false,
)

data class VideoPlayerConfigData(
    val availableResolutions: List<Resolution> = emptyList(),
    val availableVideoCodec: List<VideoCodec> = emptyList(),
    val availableAudio: List<Audio> = emptyList(),
    val availableSubtitleTracks: List<Subtitle> = emptyList(),
    val availableVideoList: List<VideoListItem> = emptyList(),
    val currentVideoCid: Long = 0,
    val currentResolution: Resolution = Resolution.R240P,
    val currentVideoCodec: VideoCodec = VideoCodec.AVC,
    val currentVideoAspectRatio: VideoAspectRatio = VideoAspectRatio.Default,
    val currentVideoSpeed: Float = 1f,
    val currentAudio: Audio = Audio.A192K,
    val currentDanmakuEnabled: Boolean = true,
    val currentDanmakuEnabledList: List<DanmakuType> = listOf(),
    val currentDanmakuSize: DanmakuSize = DanmakuSize.S2,
    val currentDanmakuScale: Float = 1f,
    val currentDanmakuTransparency: DanmakuTransparency = DanmakuTransparency.T1,
    val currentDanmakuOpacity: Float = 1f,
    val currentDanmakuArea: Float = 1f,
    val currentDanmakuMask: Boolean = false,
    val currentDanmakuLaneDensity: DanmakuLaneDensity = DanmakuLaneDensity.Standard,
    val currentDanmakuRollingDurationFactor: Float = 1f,
    val currentDanmakuFilterLevel: Int = 0,
    val currentSubtitleId: Long = 0,
    val currentSubtitleData: List<SubtitleItem> = emptyList(),
    val currentSubtitleFontSize: TextUnit = 24.sp,
    val currentSubtitleBackgroundOpacity: Float = 0.4f,
    val currentSubtitleBottomPadding: Dp = 12.dp,
    val currentPlayMode: PlayMode = PlayMode.Sequential,
    val incognitoMode: Boolean = false,
    val longPressAction: PlayerLongPressAction = PlayerLongPressAction.SpeedUp,
    val longPressSpeed: Float = 2f,
    /** PGC 片头/片尾配置；null 表示当前视频没有（UGC 恒为 null） */
    val clipInfoList: List<ClipInfo>? = null,
    /** 自动跳过 PGC 片头片尾，默认关闭 */
    val skipPgcIntroOutro: Boolean = false,
)

data class VideoPlayerDanmakuMasksData(
    val danmakuMasks: List<DanmakuMaskSegment> = emptyList(),
)

data class VideoPlayerVideoShotData(
    val videoShot: VideoShot? = null,
)

data class VideoPlayerDebugInfoData(
    val debugInfo: String = "",
)

val LocalVideoPlayerSeekData = compositionLocalOf { VideoPlayerSeekData() }
val LocalVideoPlayerSeekThumbData = compositionLocalOf { VideoPlayerSeekThumbData() }
val LocalVideoPlayerVideoInfoData = compositionLocalOf { VideoPlayerVideoInfoData() }
val LocalVideoPlayerClockData = compositionLocalOf { VideoPlayerClockData() }
val LocalVideoPlayerLogsData = compositionLocalOf { VideoPlayerLogsData() }
val LocalVideoPlayerHistoryData = compositionLocalOf { VideoPlayerHistoryData() }
val LocalVideoPlayerPaymentData = compositionLocalOf { VideoPlayerPaymentData() }
val LocalVideoPlayerInteractiveData = compositionLocalOf { VideoPlayerInteractiveData() }
val LocalVideoPlayerLoadStateData = compositionLocalOf { VideoPlayerLoadStateData() }
val LocalVideoPlayerStateData = compositionLocalOf { VideoPlayerStateData() }
val LocalVideoPlayerConfigData = compositionLocalOf { VideoPlayerConfigData() }
val LocalVideoPlayerDanmakuMasksData = compositionLocalOf { VideoPlayerDanmakuMasksData() }
val LocalVideoPlayerVideoShotData = compositionLocalOf { VideoPlayerVideoShotData() }
val LocalVideoPlayerDebugInfoData = compositionLocalOf { VideoPlayerDebugInfoData() }