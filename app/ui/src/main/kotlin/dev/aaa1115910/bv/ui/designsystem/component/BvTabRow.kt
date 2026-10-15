package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val density = LocalDensity.current
    val isTv = LocalDeviceProfile.current.isTv
    // 每个 Tab 的 x 与宽度（px）
    val bounds = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val target = bounds[selected]
    val indicatorX by animateDpAsState(
        with(density) { ((target?.first ?: 0) + (target?.second ?: 0) * 0.2f).toDp() },
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tabIndicatorX"
    )
    val indicatorW by animateDpAsState(
        with(density) { ((target?.second ?: 0) * 0.6f).toDp() },
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tabIndicatorW"
    )
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
                        .onPlaced { bounds[index] = it.positionInParent().x.toInt() to it.size.width }
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
        if (target != null) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = horizontalPadding + indicatorX)
                    .width(indicatorW)
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
            )
        }
    }
}
