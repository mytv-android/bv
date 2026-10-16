package dev.aaa1115910.bv.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.util.Prefs
import kotlin.math.roundToInt

/**
 * 「字幕」：字号、背景不透明度、底部边距。
 * 读写 Prefs 里的字幕默认值，范围沿用旧电视播放器字幕菜单（字号 12–48，背景 0–100%，底部 0–48dp）。
 */
@Composable
internal fun SubtitleSettings() {
    var fontSize by remember { mutableFloatStateOf(Prefs.defaultSubtitleFontSize.value) }
    var background by remember { mutableFloatStateOf(Prefs.defaultSubtitleBackgroundOpacity) }
    var bottom by remember { mutableFloatStateOf(Prefs.defaultSubtitleBottomPadding.value) }

    PrefSliderGroup(
        label = stringResource(R.string.settings_subtitle_size),
        valueText = stringResource(R.string.settings_subtitle_size_value, fontSize.roundToInt()),
        value = fontSize,
        onValueChange = { fontSize = it },
        save = { Prefs.defaultSubtitleFontSize = it.roundToInt().sp },
        valueRange = 12f..48f,
        steps = 35,
    )
    PrefSliderGroup(
        label = stringResource(R.string.settings_subtitle_background),
        valueText = stringResource(R.string.settings_percent, (background * 100).roundToInt()),
        value = background,
        onValueChange = { background = it },
        save = { Prefs.defaultSubtitleBackgroundOpacity = it },
        valueRange = 0f..1f,
        steps = 19,
    )
    PrefSliderGroup(
        label = stringResource(R.string.settings_subtitle_bottom),
        valueText = stringResource(R.string.settings_subtitle_bottom_value, bottom.roundToInt()),
        value = bottom,
        onValueChange = { bottom = it },
        save = { Prefs.defaultSubtitleBottomPadding = it.roundToInt().dp },
        valueRange = 0f..48f,
        steps = 47,
    )
}
