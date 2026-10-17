package dev.aaa1115910.bv.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.AudioBalanceLevel
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerLongPressAction
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.util.Prefs
import kotlin.math.abs

/** 默认倍速可选值（旧电视播放器菜单：0.25–3.0，每档 0.25）。 */
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
internal fun PlaySettings() {
    val context = LocalContext.current
    var quality by remember { mutableStateOf(Prefs.defaultQuality) }
    var codec by remember { mutableStateOf(Prefs.defaultVideoCodec) }
    var audio by remember { mutableStateOf(Prefs.defaultAudio) }
    var speed by remember { mutableFloatStateOf(Prefs.defaultPlaySpeed) }
    var playMode by remember { mutableStateOf(Prefs.defaultPlayMode) }
    var playerType by remember { mutableStateOf(Prefs.playerType) }
    var audioBalance by remember { mutableStateOf(Prefs.playerAudioBalanceLevel) }
    var ffmpegAudio by remember { mutableStateOf(Prefs.enableFfmpegAudioRenderer) }
    var skipSilence by remember { mutableStateOf(Prefs.playerSkipSilence) }
    var longPressAction by remember { mutableStateOf(Prefs.playerLongPressAction) }
    var longPressSpeed by remember { mutableFloatStateOf(Prefs.playerLongPressSpeed) }

    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_quality),
        options = Resolution.entries.reversed(),
        isSelected = { it == quality },
        optionLabel = { it.getDisplayName(context) },
        onSelect = {
            quality = it
            Prefs.defaultQuality = it
        },
    )
    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_codec),
        // 与旧电视相同：DVH1 / HVC1 不作为默认编码
        options = VideoCodec.entries.filter { it != VideoCodec.DVH1 && it != VideoCodec.HVC1 },
        isSelected = { it == codec },
        optionLabel = { it.getDisplayName(context) },
        onSelect = {
            codec = it
            Prefs.defaultVideoCodec = it
        },
    )
    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_audio),
        options = Audio.entries.toList(),
        isSelected = { it == audio },
        optionLabel = { it.getDisplayName(context) },
        onSelect = {
            audio = it
            Prefs.defaultAudio = it
        },
    )
    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_audio_balance),
        options = AudioBalanceLevel.entries.toList(),
        isSelected = { it == audioBalance },
        optionLabel = { it.getDisplayName(context) },
        onSelect = {
            audioBalance = it
            Prefs.playerAudioBalanceLevel = it
        },
    )
    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_speed),
        options = PlaySpeeds,
        isSelected = { abs(it - speed) < 0.01f },
        optionLabel = { stringResource(R.string.settings_play_speed_value, formatSpeed(it)) },
        onSelect = {
            speed = it
            Prefs.defaultPlaySpeed = it
        },
    )
    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_mode),
        options = PlayMode.entries.toList(),
        isSelected = { it == playMode },
        optionLabel = { it.getDisplayName(context) },
        onSelect = {
            playMode = it
            Prefs.defaultPlayMode = it
        },
    )
    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_player_type),
        options = PlayerType.entries.toList(),
        isSelected = { it == playerType },
        optionLabel = { it.name },
        onSelect = {
            playerType = it
            Prefs.playerType = it
        },
    )
    SettingSwitchRow(
        title = stringResource(R.string.settings_play_ffmpeg),
        subtitle = stringResource(R.string.settings_play_ffmpeg_desc),
        checked = ffmpegAudio,
        onCheckedChange = {
            ffmpegAudio = it
            Prefs.enableFfmpegAudioRenderer = it
        },
    )
    SettingSwitchRow(
        title = stringResource(R.string.settings_play_skip_silence),
        subtitle = stringResource(R.string.settings_play_skip_silence_desc),
        checked = skipSilence,
        onCheckedChange = {
            skipSilence = it
            Prefs.playerSkipSilence = it
        },
    )
    SettingChoiceGroup(
        label = stringResource(R.string.settings_play_long_press_action),
        options = PlayerLongPressAction.entries.toList(),
        isSelected = { it == longPressAction },
        optionLabel = { it.getDisplayName(context) },
        onSelect = {
            longPressAction = it
            Prefs.playerLongPressAction = it
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
                Prefs.playerLongPressSpeed = it
            },
        )
    }
}
