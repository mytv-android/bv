package com.github.mytv.bv.ui.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.focus.focusContainerColor
import com.github.mytv.bv.ui.designsystem.focus.focusContentColor
import com.github.mytv.bv.ui.designsystem.theme.BvType

data class ActionItem(
    val icon: ImageVector,
    val label: String,
    val checked: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 原型 `.m-bg` + `btnGroup()`：等宽连接按钮组（点赞 / 投币 / 收藏 / 稍后 / 分享）。
 * 未选中：surfaceContainerHigh；外侧圆角 20、内侧 8。选中：primary，四角 20（fastSpatialSpec）。
 */
@Composable
fun ActionButtonGroup(items: List<ActionItem>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { index, item ->
            ConnectedSegment(
                index = index,
                count = items.size,
                checked = item.checked,
                onClick = item.onClick,
                modifier = Modifier.weight(1f),
                verticalPadding = 0.dp,
            ) {
                Column(
                    Modifier.padding(top = 9.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        item.label,
                        style = BvType.Meta.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 原型 `segRow()`：设置里的单选组，外观同 ActionButtonGroup，只有文字（12sp），垂直内边距 10。 */
@Composable
fun SegmentedRow(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { index, label ->
            ConnectedSegment(
                index = index,
                count = options.size,
                checked = index == selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
                verticalPadding = 10.dp,
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = BvType.Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ConnectedSegment(
    index: Int,
    count: Int,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    verticalPadding: Dp,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val first = index == 0
    val last = index == count - 1
    val outer = 20.dp
    val inner = 8.dp
    val topStart by animateDpAsState(if (checked || first) outer else inner, spec, label = "segTS")
    val bottomStart by animateDpAsState(if (checked || first) outer else inner, spec, label = "segBS")
    val topEnd by animateDpAsState(if (checked || last) outer else inner, spec, label = "segTE")
    val bottomEnd by animateDpAsState(if (checked || last) outer else inner, spec, label = "segBE")
    val scheme = MaterialTheme.colorScheme
    val baseContainer by animateColorAsState(
        if (checked) scheme.primary else scheme.surfaceContainerHigh,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "segContainer"
    )
    val baseContent = if (checked) scheme.onPrimary else scheme.onSurfaceVariant
    val container by focusContainerColor(focused, baseContainer)
    val contentColor by focusContentColor(focused, baseContent)
    Surface(
        onClick = onClick,
        modifier = modifier.bvFocus(corner = outer),
        shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
        color = container,
        contentColor = contentColor,
        interactionSource = interaction,
    ) {
        Column(
            Modifier.padding(vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}
