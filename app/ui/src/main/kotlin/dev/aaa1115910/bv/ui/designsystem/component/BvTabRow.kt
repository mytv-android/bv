package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType

/**
 * 原型 `.m-tabs`：Tab 间距 22，底部 1dp outlineVariant 分隔线；选中 primary + Bold，
 * 指示条 3dp 高、宽为文字的 60%，位置变化用 defaultSpatialSpec 动画。电视上聚焦即选中。
 */
@Composable
fun BvTabRow(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    val isTv = LocalDeviceProfile.current.isTv
    // 每个 Tab 的 x 与宽度（px）。只在布局阶段写入/读取，不在 Composition 里读
    val bounds = remember { mutableStateMapOf<Int, Long>() }
    val indicatorX = remember { Animatable(0f) }
    val indicatorW = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        snapshotFlow { bounds[selected] }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { packed ->
                val x = unpackInt1(packed).toFloat()
                val width = unpackInt2(packed).toFloat()
                launch {
                    indicatorX.animateTo(
                        x + width * 0.2f,
                        MaterialTheme.motionScheme.defaultSpatialSpec<Float>(),
                    )
                }
                launch {
                    indicatorW.animateTo(
                        width * 0.6f,
                        MaterialTheme.motionScheme.defaultSpatialSpec<Float>(),
                    )
                }
            }
    }
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(horizontal = horizontalPadding)
                .then(if (isTv) Modifier.focusRestorer() else Modifier),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selected
                val color by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.motionScheme.defaultEffectsSpec(),
                    label = "tabColor"
                )
                Text(
                    title,
                    modifier = Modifier
                        .onPlaced {
                            bounds[index] = packInts(it.positionInParent().x.toInt(), it.size.width)
                        }
                        .bvFocus(corner = 8.dp, onFocused = { if (it && isTv && !isSelected) onSelect(index) })
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) }
                        .padding(top = 10.dp, bottom = 9.dp),
                    style = BvType.Body.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                    color = color,
                    maxLines = 1,
                )
            }
        }
        HorizontalDivider(
            Modifier.align(Alignment.BottomStart),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset((horizontalPadding.toPx() + indicatorX.value).roundToInt(), 0) }
                .layout { measurable, constraints ->
                    val width = indicatorW.value.roundToInt().coerceIn(0, constraints.maxWidth)
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width)
                    )
                    layout(width, placeable.height) { placeable.place(0, 0) }
                }
                .height(3.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
        )
    }
}
