package com.github.mytv.bv.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.PlayerType
import com.github.mytv.bv.player.entity.Audio
import com.github.mytv.bv.player.entity.AudioBalanceLevel
import com.github.mytv.bv.player.entity.PlayMode
import com.github.mytv.bv.player.entity.PlayerLongPressAction
import com.github.mytv.bv.player.entity.Resolution
import com.github.mytv.bv.player.entity.VideoCodec
import com.github.mytv.bv.util.Prefs
import com.github.mytv.bv.ui.PreferenceWrites
import kotlin.math.abs

/** 默认倍速可选值（旧电视播放器菜单：0.25–3.0，每档 0.25）。 */
private val QualityOptions = Resolution.entries.reversed()

private val PlaySpeeds =
    listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.25f, 2.5f, 2.75f, 3f)

/** 长按确认键加速播放的可选倍速（与旧电视播放器倍速范围一致）。 */
private val LongPressSpeeds = listOf(1.25f, 1.5f, 1.75f, 2f, 2.25f, 2.5f, 2.75f, 3f)

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()

/**
 * 「播放」：默认画质 / 视频编码 / 音质 / 倍速 / 播放模式 / 播放器内核 / 音频软解 / 跳过静音 /
 * 长按确认键行为与加速倍速。
 * 照搬旧电视 ResolutionSetting、VideoCodecSetting、AudioSetting、PlayerTypeSetting、OtherSetting（FFmpeg）
 * 与旧手机 PlayContent，单选列表改为 BvChip。
 */
@Composable
internal fun PlaySettings(container: SettingsContainer) {
    val context = LocalContext.current
    val initial = rememberPreferenceSnapshot {
        PlayPreferences(
            quality = Prefs.defaultQuality,
            codec = Prefs.defaultVideoCodec,
            audio = Prefs.defaultAudio,
            speed = Prefs.defaultPlaySpeed,
            playMode = Prefs.defaultPlayMode,
            playerType = Prefs.playerType,
            audioBalance = Prefs.playerAudioBalanceLevel,
            ffmpegAudio = Prefs.enableFfmpegAudioRenderer,
            skipSilence = Prefs.playerSkipSilence,
            skipPgcIntroOutro = Prefs.skipPgcIntroOutro,
            longPressAction = Prefs.playerLongPressAction,
            longPressSpeed = Prefs.playerLongPressSpeed,
        )
    } ?: return
    var quality by remember { mutableStateOf(initial.quality) }
    var codec by remember { mutableStateOf(initial.codec) }
    var audio by remember { mutableStateOf(initial.audio) }
    var speed by remember { mutableFloatStateOf(initial.speed) }
    var playMode by remember { mutableStateOf(initial.playMode) }
    var playerType by remember { mutableStateOf(initial.playerType) }
    var audioBalance by remember { mutableStateOf(initial.audioBalance) }
    var ffmpegAudio by remember { mutableStateOf(initial.ffmpegAudio) }
    var skipSilence by remember { mutableStateOf(initial.skipSilence) }
    var skipPgcIntroOutro by remember { mutableStateOf(initial.skipPgcIntroOutro) }
    var longPressAction by remember { mutableStateOf(initial.longPressAction) }
    var longPressSpeed by remember { mutableFloatStateOf(initial.longPressSpeed) }

    container {
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_quality),
            options = QualityOptions,
            isSelected = { it == quality },
            optionLabel = { it.getDisplayName(context) },
            onSelect = {
                quality = it
                PreferenceWrites.enqueue { Prefs.defaultQuality = it }
            },
        )
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_codec),
            // 播放器菜单里能选到的编码都可以设为默认，包括杜比视界（DVH1 / HVC1）
            options = VideoCodec.entries,
            isSelected = { it == codec },
            optionLabel = { it.getDisplayName(context) },
            onSelect = {
                codec = it
                PreferenceWrites.enqueue { Prefs.defaultVideoCodec = it }
            },
        )
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_audio),
            options = Audio.entries,
            isSelected = { it == audio },
            optionLabel = { it.getDisplayName(context) },
            onSelect = {
                audio = it
                PreferenceWrites.enqueue { Prefs.defaultAudio = it }
            },
        )
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_audio_balance),
            options = AudioBalanceLevel.entries,
            isSelected = { it == audioBalance },
            optionLabel = { it.getDisplayName(context) },
            onSelect = {
                audioBalance = it
                PreferenceWrites.enqueue { Prefs.playerAudioBalanceLevel = it }
            },
        )
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_speed),
            options = PlaySpeeds,
            isSelected = { abs(it - speed) < 0.01f },
            optionLabel = { stringResource(R.string.settings_play_speed_value, formatSpeed(it)) },
            onSelect = {
                speed = it
                PreferenceWrites.enqueue { Prefs.defaultPlaySpeed = it }
            },
        )
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_mode),
            options = PlayMode.entries,
            isSelected = { it == playMode },
            optionLabel = { it.getDisplayName(context) },
            onSelect = {
                playMode = it
                PreferenceWrites.enqueue { Prefs.defaultPlayMode = it }
            },
        )
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_player_type),
            options = PlayerType.entries,
            isSelected = { it == playerType },
            optionLabel = { it.name },
            onSelect = {
                playerType = it
                PreferenceWrites.enqueue { Prefs.playerType = it }
            },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_play_ffmpeg),
            subtitle = stringResource(R.string.settings_play_ffmpeg_desc),
            checked = ffmpegAudio,
            onCheckedChange = {
                ffmpegAudio = it
                PreferenceWrites.enqueue { Prefs.enableFfmpegAudioRenderer = it }
            },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_play_skip_silence),
            subtitle = stringResource(R.string.settings_play_skip_silence_desc),
            checked = skipSilence,
            onCheckedChange = {
                skipSilence = it
                PreferenceWrites.enqueue { Prefs.playerSkipSilence = it }
            },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_play_skip_pgc_intro_outro),
            subtitle = stringResource(R.string.settings_play_skip_pgc_intro_outro_desc),
            checked = skipPgcIntroOutro,
            onCheckedChange = {
                skipPgcIntroOutro = it
                PreferenceWrites.enqueue { Prefs.skipPgcIntroOutro = it }
            },
        )
        SettingChoiceGroup(
            label = stringResource(R.string.settings_play_long_press_action),
            options = PlayerLongPressAction.entries,
            isSelected = { it == longPressAction },
            optionLabel = { it.getDisplayName(context) },
            onSelect = {
                longPressAction = it
                PreferenceWrites.enqueue { Prefs.playerLongPressAction = it }
            },
        )
        if (longPressAction == PlayerLongPressAction.SpeedUp) {
            SettingChoiceGroup(
                label = stringResource(R.string.settings_play_long_press_speed),
                options = LongPressSpeeds,
                isSelected = { abs(it - longPressSpeed) < 0.01f },
                optionLabel = { stringResource(R.string.settings_play_speed_value, formatSpeed(it)) },
                onSelect = {
                    longPressSpeed = it
                    PreferenceWrites.enqueue { Prefs.playerLongPressSpeed = it }
                },
            )
        }
    }
}

private data class PlayPreferences(
    val quality: Resolution,
    val codec: VideoCodec,
    val audio: Audio,
    val speed: Float,
    val playMode: PlayMode,
    val playerType: PlayerType,
    val audioBalance: AudioBalanceLevel,
    val ffmpegAudio: Boolean,
    val skipSilence: Boolean,
    val skipPgcIntroOutro: Boolean,
    val longPressAction: PlayerLongPressAction,
    val longPressSpeed: Float,
)
