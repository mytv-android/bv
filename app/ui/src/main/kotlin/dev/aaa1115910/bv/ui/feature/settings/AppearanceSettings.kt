package dev.aaa1115910.bv.ui.feature.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.BvSeeds
import dev.aaa1115910.bv.entity.DarkMode
import dev.aaa1115910.bv.entity.LayoutMode
import dev.aaa1115910.bv.entity.ThemeSource
import dev.aaa1115910.bv.ui.designsystem.adaptive.BvInputMode
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.SegmentedRow
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.designsystem.theme.LocalThemePrefs
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.ui.PreferenceWrites
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/** 「配色风格」的 6 个选项（第 9.9 节，顺序固定）。 */
private val PaletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Vibrant,
    PaletteStyle.Expressive,
    PaletteStyle.Fidelity,
    PaletteStyle.Content,
    PaletteStyle.Monochrome,
)

@StringRes
private fun ThemeSource.labelRes(): Int = when (this) {
    ThemeSource.Wallpaper -> R.string.settings_source_wallpaper
    ThemeSource.Custom -> R.string.settings_source_custom
    ThemeSource.Content -> R.string.settings_source_content
}

@StringRes
private fun DarkMode.labelRes(): Int = when (this) {
    DarkMode.System -> R.string.settings_dark_system
    DarkMode.Light -> R.string.settings_dark_light
    DarkMode.Dark -> R.string.settings_dark_dark
    DarkMode.PureBlack -> R.string.settings_dark_black
}

@StringRes
private fun LayoutMode.labelRes(): Int = when (this) {
    LayoutMode.Auto -> R.string.settings_appearance_layout_auto
    LayoutMode.Phone -> R.string.settings_appearance_layout_phone
    LayoutMode.Tablet -> R.string.settings_appearance_layout_tablet
    LayoutMode.Tv -> R.string.settings_appearance_layout_tv
}

@StringRes
private fun PaletteStyle.labelRes(): Int = when (this) {
    PaletteStyle.TonalSpot -> R.string.settings_style_tonal_spot
    PaletteStyle.Vibrant -> R.string.settings_style_vibrant
    PaletteStyle.Expressive -> R.string.settings_style_expressive
    PaletteStyle.Fidelity -> R.string.settings_style_fidelity
    PaletteStyle.Content -> R.string.settings_style_content
    PaletteStyle.Monochrome -> R.string.settings_style_monochrome
    else -> R.string.settings_style_vibrant
}

@StringRes
private fun contrastLabel(value: Float): Int = when {
    value < .25f -> R.string.settings_contrast_standard
    value < .75f -> R.string.settings_contrast_medium
    else -> R.string.settings_contrast_high
}

/**
 * 「外观与主题」（第 9.9 节，原型 appearance()；三端相同内容）：
 * 界面布局、主题来源、种子色、配色风格、对比度、深色模式（电视隐藏）、封面颜色、减少动效、界面缩放（仅电视）。
 * 读 [LocalThemePrefs]，每一项修改立即调用 Prefs.setXxx，整个 App 实时换色。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSettings(gapColor: Color) {
    val prefs = LocalThemePrefs.current
    val profile = LocalDeviceProfile.current
    val scope = rememberCoroutineScope()
    var showCustomSeed by remember { mutableStateOf(false) }

    // 1. 界面布局：自动 / 手机竖屏 / 平板横屏 / 电视横屏，选完立刻切换整个 App 的布局
    val layoutModeFlow = remember { Prefs.layoutModeFlow }
    val layoutMode by layoutModeFlow.collectAsStateWithLifecycle(initialValue = LayoutMode.Auto)
    val modes = LayoutMode.entries
    SettingGroup(stringResource(R.string.settings_appearance_layout)) {
        SegmentedRow(
            options = modes.map { stringResource(it.labelRes()) },
            selected = modes.indexOf(layoutMode),
            onSelect = { index -> PreferenceWrites.enqueue { Prefs.layoutMode = modes[index] } },
            modifier = Modifier
                .focusRestorer()
                .focusGroup(),
        )
    }

    val pickSeed: (Color) -> Unit = { color ->
        scope.launch {
            Prefs.setSeedColor(color)
            // 选了种子色就切到「自选种子色」，否则壁纸 / 内容来源下看不到变化
            if (prefs.source != ThemeSource.Custom) Prefs.setThemeSource(ThemeSource.Custom)
        }
    }

    // 2. 主题来源（Android 12 以下没有壁纸取色，隐藏「系统壁纸」）
    val sources = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ThemeSource.entries.toList()
        else listOf(ThemeSource.Custom, ThemeSource.Content)
    }
    SettingGroup(stringResource(R.string.settings_appearance_source)) {
        SegmentedRow(
            options = sources.map { stringResource(it.labelRes()) },
            selected = sources.indexOf(prefs.source),
            onSelect = { index -> scope.launch { Prefs.setThemeSource(sources[index]) } },
            modifier = Modifier
                .focusRestorer()
                .focusGroup(),
        )
    }

    // 3. 种子色：6 个预设圆点 + 自定义（+）
    val seedArgb = prefs.seed.toArgb()
    val isPreset = BvSeeds.presets.any { it.toArgb() == seedArgb }
    SettingGroup(stringResource(R.string.settings_appearance_seed)) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            BvSeeds.presets.forEachIndexed { index, color ->
                SeedSwatch(
                    color = color,
                    selected = color.toArgb() == seedArgb,
                    gapColor = gapColor,
                    description = stringResource(R.string.settings_appearance_seed_preset, index + 1),
                    onClick = { pickSeed(color) },
                )
            }
            if (!isPreset) {
                SeedSwatch(
                    color = prefs.seed,
                    selected = true,
                    gapColor = gapColor,
                    description = stringResource(R.string.settings_appearance_seed_custom),
                    onClick = { showCustomSeed = true },
                )
            }
            BvIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.settings_appearance_seed_custom),
                onClick = { showCustomSeed = true },
                style = BvIconButtonStyle.Tonal,
            )
        }
    }

    // 4. 配色风格
    SettingChoiceGroup(
        label = stringResource(R.string.settings_appearance_style),
        options = PaletteStyles,
        isSelected = { it == prefs.style },
        optionLabel = { stringResource(it.labelRes()) },
        onSelect = { style -> scope.launch { Prefs.setPaletteStyle(style) } },
    )

    // 5. 对比度：3 档 0 / 0.5 / 1.0
    var contrast by remember(prefs.contrast) { mutableFloatStateOf(prefs.contrast.toFloat()) }
    SettingSliderGroup(
        label = stringResource(R.string.settings_appearance_contrast),
        valueText = stringResource(contrastLabel(contrast)),
        value = contrast,
        onValueChange = { v ->
            val snapped = ((v * 2).roundToInt() / 2f).coerceIn(0f, 1f)
            if (snapped != contrast) {
                contrast = snapped
                scope.launch { Prefs.setContrast(snapped) }
            }
        },
        valueRange = 0f..1f,
        steps = 1,
    )

    // 6. 深色模式（电视永远深色，整项隐藏）
    if (!profile.isTv) {
        val modes = DarkMode.entries
        SettingGroup(stringResource(R.string.settings_appearance_dark)) {
            SegmentedRow(
                options = modes.map { stringResource(it.labelRes()) },
                selected = modes.indexOf(prefs.darkMode),
                onSelect = { index -> scope.launch { Prefs.setDarkMode(modes[index]) } },
                modifier = Modifier
                    .focusRestorer()
                    .focusGroup(),
            )
        }
    }

    // 7. 详情页使用封面颜色
    SettingSwitchRow(
        title = stringResource(R.string.settings_appearance_cover),
        subtitle = stringResource(R.string.settings_appearance_cover_desc),
        checked = prefs.coverTheme,
        onCheckedChange = { v -> scope.launch { Prefs.setCoverTheme(v) } },
    )

    // 8. 减少动效
    SettingSwitchRow(
        title = stringResource(R.string.settings_appearance_reduce_motion),
        subtitle = stringResource(R.string.settings_appearance_reduce_motion_desc),
        checked = prefs.reduceMotion,
        onCheckedChange = { v -> scope.launch { Prefs.setReduceMotion(v) } },
    )

    // 9. 界面缩放（仅电视）
    if (profile.isTv) TvScaleSetting()

    if (showCustomSeed) {
        CustomSeedDialog(
            initial = prefs.seed,
            onDismiss = { showCustomSeed = false },
            onConfirm = { color ->
                showCustomSeed = false
                pickSeed(color)
            },
        )
    }
}

/**
 * 种子色圆点：32dp 圆形；选中时 36dp、圆角 30%、旋转 45°，外加 2dp 面板色间隙 + 2dp onSurface 外圈
 * （原型 swatches() 的 box-shadow），用 defaultSpatialSpec 过渡；减少动效时直接跳到终态。
 */
@Composable
private fun SeedSwatch(
    color: Color,
    selected: Boolean,
    gapColor: Color,
    description: String,
    onClick: () -> Unit,
) {
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val target = if (selected) 1f else 0f
    val animated by animateFloatAsState(target, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "seedSwatch")
    val progress = { if (reduceMotion) target else animated }
    val ringColor = MaterialTheme.colorScheme.onSurface
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .layout { measurable, constraints ->
                val dotSize = (32f + 4f * progress()).coerceIn(28f, 40f).dp.roundToPx()
                val width = constraints.constrainWidth(dotSize)
                val height = constraints.constrainHeight(dotSize)
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .bvFocus(corner = 999.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .graphicsLayer { rotationZ = 45f * progress() }
            .drawBehind {
                val ring = progress().coerceIn(0f, 1f)
                val radius = size.minDimension * (0.5f - 0.2f * ring)
                if (ring > 0f) {
                    val outer = 4.dp.toPx()
                    val gap = 2.dp.toPx()
                    drawRoundRect(
                        color = ringColor.copy(alpha = ringColor.alpha * ring),
                        topLeft = Offset(-outer, -outer),
                        size = Size(size.width + outer * 2, size.height + outer * 2),
                        cornerRadius = CornerRadius(radius + outer),
                    )
                    drawRoundRect(
                        color = gapColor.copy(alpha = gapColor.alpha * ring),
                        topLeft = Offset(-gap, -gap),
                        size = Size(size.width + gap * 2, size.height + gap * 2),
                        cornerRadius = CornerRadius(radius + gap),
                    )
                }
                drawRoundRect(color = color, cornerRadius = CornerRadius(radius))
            },
    )
}

/** 自定义种子色：色相滑块 0–360 + 预览（HSV 饱和度 0.7、明度 0.95）。 */
@Composable
private fun CustomSeedDialog(initial: Color, onDismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    val dpad = LocalDeviceProfile.current.input == BvInputMode.DPad
    var hue by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initial.toArgb(), hsv)
        mutableFloatStateOf(hsv[0])
    }
    val preview = Color.hsv(hue.coerceIn(0f, 360f), 0.7f, 0.95f)
    val sliderRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { sliderRequester.requestFocus() }
    }
    SettingsDialog(onDismiss) {
        Text(stringResource(R.string.settings_appearance_seed_custom), style = BvType.PanelTitle)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(preview)
            )
            Text(
                String.format(Locale.ROOT, "#%06X", preview.toArgb() and 0xFFFFFF),
                style = BvType.Title.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        SettingSliderGroup(
            label = stringResource(R.string.settings_appearance_hue),
            valueText = stringResource(R.string.settings_appearance_hue_value, hue.roundToInt()),
            value = hue,
            onValueChange = { hue = it.coerceIn(0f, 360f) },
            valueRange = 0f..360f,
            // 方向键每次 10°；触摸连续拖动
            steps = if (dpad) 35 else 0,
            sliderModifier = Modifier.focusRequester(sliderRequester),
        )
        SettingsDialogActions(
            confirm = stringResource(R.string.settings_confirm),
            onConfirm = { onConfirm(preview) },
            dismiss = stringResource(R.string.settings_cancel),
            onDismiss = onDismiss,
        )
    }
}

/**
 * 界面缩放（仅电视）：写 Prefs.density。范围沿用旧电视 UI 设置（0.5–5.0，每档 0.1），
 * 百分比以 960dp 宽为 100%。
 */
@Composable
private fun TvScaleSetting() {
    val context = LocalContext.current
    val current = LocalDensity.current.density
    val base = remember(context) { context.resources.displayMetrics.widthPixels / 960f }
    var value by remember { mutableFloatStateOf(((current * 10).roundToInt() / 10f).coerceIn(0.5f, 5f)) }
    val percent = if (base > 0f) (value / base * 100).roundToInt() else 100
    PrefSliderGroup(
        label = stringResource(R.string.settings_appearance_scale),
        valueText = stringResource(R.string.settings_appearance_scale_value, percent),
        value = value,
        onValueChange = { v ->
            val rounded = ((v * 10).roundToInt() / 10f).coerceIn(0.5f, 5f)
            if (rounded != value) {
                value = rounded
            }
        },
        save = { Prefs.density = ((it * 10).roundToInt() / 10f).coerceIn(0.5f, 5f) },
        valueRange = 0.5f..5f,
        steps = 44,
    )
}
