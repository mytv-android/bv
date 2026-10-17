@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.danmaku.DanmakuLaneDensity
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData

/**
 * 播放器菜单的统一状态（第 11 节第 5 条）：分辨率、编码、音质、倍速、画面比例、
 * 弹幕开关 / 类型 / 大小 / 透明度 / 区域 / 蒙版 / 密度 / 等级过滤、字幕轨道 / 字号 / 背景 / 边距、播放模式。
 *
 * 触摸端的侧边面板和电视端的菜单面板都读写它：
 * - 读：当前值来自调用方提供的 `LocalVideoPlayerConfigData`（即 VideoPlayerV3ViewModel 的状态），
 *   由 [BvPlayerScreen] 每次组合时同步进来；
 * - 写：调用 `select*` / `set*`，先做播放器侧的处理（切换后回到原进度、更新弹幕配置、调整画面比例…），
 *   再回调 [BvPlayerScreen] 上对应的 `on*Change` 回调，由调用方持久化到 ViewModel / Prefs。
 *
 * 一般不需要自己创建；需要在播放器外部读取或修改菜单值时，用 [rememberPlayerMenuState] 创建后传给 [BvPlayerScreen]。
 */
@Stable
class PlayerMenuState internal constructor() {
    internal var config: VideoPlayerConfigData by mutableStateOf(VideoPlayerConfigData())
    internal var handlers: PlayerMenuHandlers? = null

    // ---------- 画面 / 音频 ----------
    /** 可选分辨率，从高到低。 */
    val availableResolutions: List<Resolution> get() = config.availableResolutions.sortedByDescending { it.code }
    val resolution: Resolution get() = config.currentResolution
    val availableVideoCodecs: List<VideoCodec> get() = config.availableVideoCodec
    val videoCodec: VideoCodec get() = config.currentVideoCodec

    /** 可选音质，从高到低。 */
    val availableAudios: List<Audio> get() = config.availableAudio.sortedByDescending { it.ordinal }
    val audio: Audio get() = config.currentAudio
    val speed: Float get() = config.currentVideoSpeed
    val aspectRatio: VideoAspectRatio get() = config.currentVideoAspectRatio

    // ---------- 弹幕 ----------
    val danmakuEnabled: Boolean get() = config.currentDanmakuEnabled
    val danmakuTypes: List<DanmakuType> get() = config.currentDanmakuEnabledList
    val danmakuScale: Float get() = config.currentDanmakuScale
    val danmakuOpacity: Float get() = config.currentDanmakuOpacity
    val danmakuArea: Float get() = config.currentDanmakuArea
    val danmakuMask: Boolean get() = config.currentDanmakuMask
    val danmakuLaneDensity: DanmakuLaneDensity get() = config.currentDanmakuLaneDensity
    val danmakuFilterLevel: Int get() = config.currentDanmakuFilterLevel

    // ---------- 字幕 ----------
    val subtitleTracks: List<Subtitle> get() = config.availableSubtitleTracks
    val subtitleId: Long get() = config.currentSubtitleId
    val subtitleFontSize: TextUnit get() = config.currentSubtitleFontSize
    val subtitleBackgroundOpacity: Float get() = config.currentSubtitleBackgroundOpacity
    val subtitleBottomPadding: Dp get() = config.currentSubtitleBottomPadding

    // ---------- 其他 ----------
    val playMode: PlayMode get() = config.currentPlayMode
    val videoList: List<VideoListItem> get() = config.availableVideoList
    val currentVideoCid: Long get() = config.currentVideoCid

    /** 某类弹幕当前是否显示（「全部」包含所有类型）。 */
    fun isDanmakuTypeShown(type: DanmakuType): Boolean =
        danmakuTypes.contains(DanmakuType.All) || danmakuTypes.contains(type)

    fun selectResolution(resolution: Resolution) {
        handlers?.onResolutionChange?.invoke(resolution)
    }

    fun selectVideoCodec(codec: VideoCodec) {
        handlers?.onCodecChange?.invoke(codec)
    }

    fun selectAudio(audio: Audio) {
        handlers?.onAudioChange?.invoke(audio)
    }

    fun setSpeed(speed: Float) {
        handlers?.onPlaySpeedChange?.invoke(speed)
    }

    fun setAspectRatio(aspectRatio: VideoAspectRatio) {
        handlers?.onAspectRatioChange?.invoke(aspectRatio)
    }

    fun setDanmakuEnabled(enabled: Boolean) {
        handlers?.onDanmakuEnabledChange?.invoke(enabled)
    }

    fun setDanmakuTypes(types: List<DanmakuType>) {
        handlers?.onDanmakuTypesChange?.invoke(types)
    }

    /**
     * 切换某类弹幕的显示。规则照搬旧电视菜单：勾选「全部」= 全部类型；取消「全部」= 空；
     * 在「全部」状态下取消某一项会去掉「全部」；其余三项都勾上时自动补上「全部」。
     */
    fun toggleDanmakuType(type: DanmakuType) {
        val current = danmakuTypes
        if (type == DanmakuType.All) {
            setDanmakuTypes(if (current.contains(DanmakuType.All)) emptyList() else DanmakuType.entries)
            return
        }
        val concrete = DanmakuType.entries - DanmakuType.All
        val base = if (current.contains(DanmakuType.All)) concrete else current.filter { it != DanmakuType.All }
        val next = if (base.contains(type)) base - type else base + type
        setDanmakuTypes(if (next.containsAll(concrete)) DanmakuType.entries else next)
    }

    fun setDanmakuScale(scale: Float) {
        handlers?.onDanmakuScaleChange?.invoke(scale)
    }

    fun setDanmakuOpacity(opacity: Float) {
        handlers?.onDanmakuOpacityChange?.invoke(opacity)
    }

    fun setDanmakuArea(area: Float) {
        handlers?.onDanmakuAreaChange?.invoke(area)
    }

    fun setDanmakuMask(enabled: Boolean) {
        handlers?.onDanmakuMaskChange?.invoke(enabled)
    }

    fun setDanmakuLaneDensity(density: DanmakuLaneDensity) {
        handlers?.onDanmakuLaneDensityChange?.invoke(density)
    }

    fun setDanmakuFilterLevel(level: Int) {
        handlers?.onDanmakuFilterLevelChange?.invoke(level)
    }

    fun selectSubtitle(subtitle: Subtitle) {
        handlers?.onSubtitleChange?.invoke(subtitle)
    }

    fun setSubtitleFontSize(size: TextUnit) {
        handlers?.onSubtitleSizeChange?.invoke(size)
    }

    fun setSubtitleBackgroundOpacity(opacity: Float) {
        handlers?.onSubtitleBackgroundOpacityChange?.invoke(opacity)
    }

    fun setSubtitleBottomPadding(padding: Dp) {
        handlers?.onSubtitleBottomPaddingChange?.invoke(padding)
    }

    fun setPlayMode(playMode: PlayMode) {
        handlers?.onPlayModeChange?.invoke(playMode)
    }

    companion object {
        /** 倍速常用值（触摸端标签、电视端选项）。 */
        val SpeedPresets: List<Float> =
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.25f, 2.5f, 2.75f, 3f)

        /** 触摸端倍速滑块范围。 */
        val SpeedRange: ClosedFloatingPointRange<Float> = 0.5f..3f
    }
}

/** 菜单写入时的处理（播放器侧逻辑 + 调用方回调），由 [BvPlayerScreen] 绑定。 */
internal class PlayerMenuHandlers(
    val onResolutionChange: (Resolution) -> Unit,
    val onCodecChange: (VideoCodec) -> Unit,
    val onAudioChange: (Audio) -> Unit,
    val onPlaySpeedChange: (Float) -> Unit,
    val onAspectRatioChange: (VideoAspectRatio) -> Unit,
    val onDanmakuEnabledChange: (Boolean) -> Unit,
    val onDanmakuTypesChange: (List<DanmakuType>) -> Unit,
    val onDanmakuScaleChange: (Float) -> Unit,
    val onDanmakuOpacityChange: (Float) -> Unit,
    val onDanmakuAreaChange: (Float) -> Unit,
    val onDanmakuMaskChange: (Boolean) -> Unit,
    val onDanmakuLaneDensityChange: (DanmakuLaneDensity) -> Unit,
    val onDanmakuFilterLevelChange: (Int) -> Unit,
    val onSubtitleChange: (Subtitle) -> Unit,
    val onSubtitleSizeChange: (TextUnit) -> Unit,
    val onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    val onSubtitleBottomPaddingChange: (Dp) -> Unit,
    val onPlayModeChange: (PlayMode) -> Unit,
)

/** 创建一个 [PlayerMenuState]，传给 [BvPlayerScreen] 的 `menuState` 参数。 */
@Composable
fun rememberPlayerMenuState(): PlayerMenuState = remember { PlayerMenuState() }
