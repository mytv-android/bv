package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.focus.focusContainerColor
import dev.aaa1115910.bv.ui.designsystem.focus.focusContentColor
import dev.aaa1115910.bv.ui.designsystem.focus.showFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType

/**
 * 原型 `.m-chip`：未选中 圆角 10 + 1dp outlineVariant 内描边；选中 全圆角 + secondaryContainer。
 * selectOnFocus = true 时聚焦即选中（电视顶栏标签）。[trailing] 放在文字后面（下拉箭头、数量角标）。
 */
@Composable
fun BvChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selectOnFocus: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    textStyle: TextStyle = BvType.Small.copy(fontWeight = FontWeight.Medium),
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val corner by animateDpAsState(
        if (selected) 100.dp else 10.dp,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "chipCorner"
    )
    val baseContainer by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "chipContainer"
    )
    val baseContent = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val container by focusContainerColor(focused, baseContainer)
    val content by focusContentColor(focused, baseContent)
    val shape = RoundedCornerShape(corner)
    Surface(
        onClick = onClick,
        modifier = modifier.bvFocus(onFocused = { if (it && selectOnFocus && !selected) onClick() }),
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = content,
        border = if (selected || showFocus(focused)) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = textStyle, maxLines = 1)
            trailing?.invoke()
        }
    }
}
