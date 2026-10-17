package dev.aaa1115910.bv.ui.feature.settings

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.player.danmaku.DanmakuLaneDensity
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.util.Prefs
import kotlin.math.roundToInt

/**
 * 勾选弹幕类型后的结果（照抄旧电视播放器 DanmakuMenu 的「全部」联动规则）。
 * current = 原来的选择，selected = 用户这次勾选后的列表。
 */
private fun resolveDanmakuTypes(current: List<DanmakuType>, selected: List<DanmakuType>): List<DanmakuType> {
    val all = DanmakuType.All
    return when {
        // 勾选了全部
        all in selected && all !in current -> DanmakuType.entries.toList()
        // 取消了全部
        all in current && all !in selected -> emptyList()
        // 在勾选全部时，取消某一项
        all in current && all in selected && current.size != selected.size -> selected - all
        // 勾选了全部之外的所有项时，勾选全部项
        all !in current && selected.size == DanmakuType.entries.size - 1 -> DanmakuType.entries.toList()
        else -> selected
    }
}

/**
 * 「弹幕」：默认开关、显示类型、密度、大小、速度、不透明度、显示区域、智能防挡、等级过滤。
 * 读写 Prefs 里的弹幕默认值（与新播放器 PlayerHost 写入的是同一组键），范围沿用旧电视播放器菜单。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DanmakuSettings() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(Prefs.defaultDanmakuEnabled) }
    var types by remember { mutableStateOf(Prefs.defaultDanmakuTypes) }
    var scale by remember { mutableFloatStateOf(Prefs.defaultDanmakuScale) }
    var opacity by remember { mutableFloatStateOf(Prefs.defaultDanmakuOpacity) }
    var area by remember { mutableFloatStateOf(Prefs.defaultDanmakuArea) }
    var mask by remember { mutableStateOf(Prefs.defaultDanmakuMask) }
    var laneDensity by remember { mutableStateOf(Prefs.defaultDanmakuLaneDensity) }
    var rollingDurationFactor by remember { mutableFloatStateOf(Prefs.defaultDanmakuRollingDurationFactor) }
    var filterLevel by remember { mutableIntStateOf(Prefs.defaultDanmakuFilterLevel) }

    SettingSwitchRow(
        title = stringResource(R.string.settings_danmaku_enabled),
        subtitle = stringResource(R.string.settings_danmaku_enabled_desc),
        checked = enabled,
        onCheckedChange = {
            enabled = it
            Prefs.defaultDanmakuEnabled = it
        },
    )

    SettingGroup(stringResource(R.string.settings_danmaku_types)) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DanmakuType.entries.forEach { type ->
                val on = type in types
                BvChip(
                    text = type.getDisplayName(context),
                    selected = on,
                    onClick = {
                        val selected = if (on) types - type else types + type
                        types = resolveDanmakuTypes(types, selected)
                        Prefs.defaultDanmakuTypes = types
                    },
                )
            }
        }
    }

    SettingGroup(stringResource(R.string.settings_danmaku_density)) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DanmakuLaneDensity.entries.forEach { density ->
                BvChip(
                    text = stringResource(
                        when (density) {
                            DanmakuLaneDensity.Sparse -> R.string.settings_danmaku_density_sparse
                            DanmakuLaneDensity.Standard -> R.string.settings_danmaku_density_standard
                            DanmakuLaneDensity.Dense -> R.string.settings_danmaku_density_dense
                        }
                    ),
                    selected = laneDensity == density,
                    onClick = {
                        laneDensity = density
                        Prefs.defaultDanmakuLaneDensity = density
                    },
                )
            }
        }
    }

    PrefSliderGroup(
        label = stringResource(R.string.settings_danmaku_scale),
        valueText = stringResource(R.string.settings_percent, (scale * 100).roundToInt()),
        value = scale,
        onValueChange = { scale = it },
        save = { Prefs.defaultDanmakuScale = it },
        valueRange = 0.5f..4f,
        steps = 69,
    )
    PrefSliderGroup(
        label = stringResource(R.string.settings_danmaku_speed),
        valueText = "${(rollingDurationFactor * 10).roundToInt() / 10f}x",
        value = rollingDurationFactor,
        onValueChange = { rollingDurationFactor = it },
        save = { Prefs.defaultDanmakuRollingDurationFactor = it },
        valueRange = 0.2f..1.8f,
        steps = 15,
    )
    PrefSliderGroup(
        label = stringResource(R.string.settings_danmaku_opacity),
        valueText = stringResource(R.string.settings_percent, (opacity * 100).roundToInt()),
        value = opacity,
        onValueChange = { opacity = it },
        save = { Prefs.defaultDanmakuOpacity = it },
        valueRange = 0f..1f,
        steps = 19,
    )
    PrefSliderGroup(
        label = stringResource(R.string.settings_danmaku_area),
        valueText = stringResource(R.string.settings_percent, (area * 100).roundToInt()),
        value = area,
        onValueChange = { area = it },
        save = { Prefs.defaultDanmakuArea = it },
        valueRange = 0f..1f,
        steps = 19,
    )

    SettingSwitchRow(
        title = stringResource(R.string.settings_danmaku_mask),
        subtitle = stringResource(R.string.settings_danmaku_mask_desc),
        checked = mask,
        onCheckedChange = {
            mask = it
            Prefs.defaultDanmakuMask = it
        },
    )

    PrefSliderGroup(
        label = stringResource(R.string.settings_danmaku_filter_level),
        valueText = stringResource(R.string.settings_danmaku_filter_level_value, filterLevel),
        value = filterLevel.toFloat(),
        onValueChange = { filterLevel = it.roundToInt() },
        save = { Prefs.defaultDanmakuFilterLevel = it.roundToInt() },
        valueRange = 0f..10f,
        steps = 9,
    )
}
