package com.github.mytv.bv.ui.feature.settings

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.github.mytv.bv.ui.PreferenceWrites
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvChip
import com.github.mytv.bv.ui.designsystem.component.BvListItem
import com.github.mytv.bv.ui.designsystem.component.BvSlider
import com.github.mytv.bv.ui.designsystem.component.BvSwitch
import com.github.mytv.bv.ui.designsystem.focus.BvKey
import com.github.mytv.bv.ui.designsystem.focus.onBvKey
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.navigation.AccountsRoute
import com.github.mytv.bv.ui.navigation.LogsRoute
import com.github.mytv.bv.ui.navigation.MediaCodecRoute
import com.github.mytv.bv.ui.navigation.SpeedTestRoute
import com.github.mytv.bv.ui.navigation.UserLockRoute
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/** 请求焦点（组节点会把焦点交给第一个子项）；失败时不抛异常。 */
internal fun FocusRequester.tryFocusEnter(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/** 分类项（原型 catRow）：BvListItem，40dp 图标容器裁剪为 Cookie9Sided，颜色按分类轮换。 */
@Composable
internal fun SettingsCategoryItem(
    category: SettingsCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: ((Boolean) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val container = when (category.role) {
        CategoryRole.Primary -> scheme.primaryContainer
        CategoryRole.Secondary -> scheme.secondaryContainer
        CategoryRole.Tertiary -> scheme.tertiaryContainer
    }
    val content = when (category.role) {
        CategoryRole.Primary -> scheme.onPrimaryContainer
        CategoryRole.Secondary -> scheme.onSecondaryContainer
        CategoryRole.Tertiary -> scheme.onTertiaryContainer
    }
    BvListItem(
        title = stringResource(category.title),
        modifier = modifier.fillMaxWidth(),
        subtitle = stringResource(category.subtitle),
        icon = category.icon(),
        iconContainerColor = container,
        iconContentColor = content,
        iconShape = MaterialShapes.Cookie9Sided.toShape(),
        selected = selected,
        onClick = onClick,
        onFocused = onFocused,
    )
}

/**
 * 分类详情内容（三端相同，由外层 Column 决定间距：手机 / 平板 22，电视 16）。
 * paneColor 是内容所在面板的背景色（种子色选中圈的 2dp 间隙用它画）。
 */
internal typealias SettingsContainer = @Composable (@Composable () -> Unit) -> Unit

@Composable
internal fun SettingsCategoryContent(
    category: SettingsCategory,
    nav: NavController,
    paneColor: Color,
    container: SettingsContainer,
) {
    // Mount the scroll container only after preferences are ready. A loading-only
    // measure would clamp a restored ScrollState to zero before controls arrive.
    when (category) {
        SettingsCategory.Appearance -> container { AppearanceSettings(gapColor = paneColor) }
        SettingsCategory.Play -> PlaySettings(container)
        SettingsCategory.Danmaku -> DanmakuSettings(container)
        SettingsCategory.Subtitle -> SubtitleSettings(container)
        SettingsCategory.Network -> NetworkSettings(onSpeedTest = { nav.navigate(SpeedTestRoute) }, container = container)
        SettingsCategory.Account -> AccountSettings(
            onAccounts = { nav.navigate(AccountsRoute) },
            onUserLock = { nav.navigate(UserLockRoute) },
            container = container,
        )

        SettingsCategory.Storage -> StorageSettings(onLogs = { nav.navigate(LogsRoute) }, container = container)
        SettingsCategory.About -> AboutSettings(onMediaCodec = { nav.navigate(MediaCodecRoute) }, container = container)
    }
}

private val GroupLabelStyle = BvType.Small.copy(fontWeight = FontWeight.Bold)
private val SwitchTitleStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
private val RowBleed = 12.dp

/** 组标签：13sp Bold onSurfaceVariant（原型 `<b style="font-size:13px;color:var(--on-surface-variant)">`）。 */
@Composable
internal fun SettingLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = GroupLabelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** 一组设置：Column(spacedBy 8)：标签（可带右侧当前值，13sp 等宽数字）+ 控件。 */
@Composable
internal fun SettingGroup(
    label: String,
    value: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (value == null) {
            SettingLabel(label)
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingLabel(label, Modifier.weight(1f, fill = false))
                Text(
                    value,
                    style = BvType.Small.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        content()
    }
}

/** 开关行：Row(SpaceBetween)：Column(14sp Bold 标题 + Meta 说明) + Switch。 */
@Composable
internal fun SettingSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = SwitchTitleStyle, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = BvType.Meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BvSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 单选组：标签 + FlowRow 的 BvChip（原型「配色风格」）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> SettingChoiceGroup(
    label: String,
    options: List<T>,
    isSelected: (T) -> Boolean,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    SettingGroup(label) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                BvChip(
                    text = optionLabel(option),
                    selected = isSelected(option),
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

/**
 * BvSlider。方向键上 / 下不交给滑块（M3 Slider 会把它们当作调节），而是移动焦点，
 * 这样电视上滑块不会把焦点困住；左 / 右照常调节数值。
 */
@Composable
internal fun SettingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    BvSlider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        modifier = modifier.onBvKey { key ->
            when (key) {
                BvKey.Up -> {
                    focusManager.moveFocus(FocusDirection.Up)
                    true
                }

                BvKey.Down -> {
                    focusManager.moveFocus(FocusDirection.Down)
                    true
                }

                else -> false
            }
        },
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
    )
}

/** 滑块组：Row(SpaceBetween)(标签 + 当前值) + 滑块（原型「对比度」）。 */
@Composable
internal fun SettingSliderGroup(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    sliderModifier: Modifier = Modifier,
) {
    SettingGroup(label, valueText) {
        SettingSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = sliderModifier,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

/**
 * 写入 Prefs 的滑块组：拖动时只改本地值，松手（或方向键调节停下 400ms 后）再写入，
 * 避免每一帧都写 DataStore。
 */
private class SavedSliderValue(var value: Float) {
    var current = value
}

@OptIn(FlowPreview::class)
@Composable
internal fun PrefSliderGroup(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    save: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    val currentValue by rememberUpdatedState(value)
    val currentSave by rememberUpdatedState(save)
    val saved = remember { SavedSliderValue(value) }
    SideEffect { saved.current = value }
    val commit: () -> Unit = {
        val next = saved.current
        if (next != saved.value) {
            saved.value = next
            val saveValue = currentSave
            PreferenceWrites.enqueue { saveValue(next) }
        }
    }
    val currentCommit by rememberUpdatedState(commit)
    LaunchedEffect(Unit) {
        snapshotFlow { currentValue }.debounce(400).collect { currentCommit() }
    }
    DisposableEffect(Unit) {
        onDispose { currentCommit() }
    }
    SettingSliderGroup(
        label = label,
        valueText = valueText,
        value = value,
        onValueChange = {
            saved.current = it
            onValueChange(it)
        },
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = commit,
    )
}

/**
 * 让列表行的背景向左右各多出 12dp，文字仍与其他设置项左对齐。
 * （行自身有 12dp 水平内边距；外层内容区至少有 16dp 内边距，不会被裁切。）
 */
internal fun Modifier.settingBleed(): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        val extra = RowBleed.roundToPx()
        val width = constraints.maxWidth + extra * 2
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
    }
}

/** 可点击的设置行（BvListItem）：标题 + 说明，右侧可选当前值与 › 箭头。 */
@Composable
internal fun SettingNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    showChevron: Boolean = true,
) {
    BvListItem(
        title = title,
        modifier = modifier
            .settingBleed()
            .fillMaxWidth(),
        subtitle = subtitle,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = RowBleed, vertical = 10.dp),
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value != null) {
                    Text(
                        value,
                        style = BvType.Label.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"),
                        color = LocalContentColor.current.copy(alpha = .72f),
                        maxLines = 1,
                    )
                }
                if (showChevron) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        },
    )
}

/** 一组紧挨着的设置行：标签 + Column(spacedBy 2)。 */
@Composable
internal fun SettingRows(label: String, content: @Composable ColumnScope.() -> Unit) {
    SettingGroup(label) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

/** 设置里的对话框外框：圆角 28、surfaceContainerHigh、内边距 24、Column(spacedBy 16)。 */
@Composable
internal fun SettingsDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                Modifier
                    .widthIn(min = 280.dp, max = 520.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        }
    }
}

/** 对话框底部按钮：右对齐，次要按钮（描边）在左、主要按钮（实心）在右。 */
@Composable
internal fun ColumnScope.SettingsDialogActions(
    confirm: String?,
    onConfirm: () -> Unit,
    dismiss: String?,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    confirmRequester: FocusRequester? = null,
    dismissRequester: FocusRequester? = null,
) {
    Row(
        Modifier
            .align(Alignment.End)
            .focusRestorer()
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (dismiss != null) {
            BvButton(
                text = dismiss,
                onClick = onDismiss,
                modifier = if (dismissRequester != null) Modifier.focusRequester(dismissRequester) else Modifier,
                style = BvButtonStyle.Outlined,
                enabled = dismissEnabled,
            )
        }
        if (confirm != null) {
            BvButton(
                text = confirm,
                onClick = onConfirm,
                modifier = if (confirmRequester != null) Modifier.focusRequester(confirmRequester) else Modifier,
                enabled = confirmEnabled,
            )
        }
    }
}
