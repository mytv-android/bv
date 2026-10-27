package com.github.mytv.bv.ui.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.focus.focusContainerColor
import com.github.mytv.bv.ui.designsystem.focus.focusContentColor
import com.github.mytv.bv.ui.designsystem.focus.showFocus
import com.github.mytv.bv.ui.designsystem.theme.BvType

/** 原型 `.m-btn` 的三种样式：fill（primary）、t（secondaryContainer）、o（描边）。 */
enum class BvButtonStyle { Filled, Tonal, Outlined }

/**
 * 原型 `.m-btn`：高 40，水平内边距 20，全圆角，14sp SemiBold，图标 20dp 间距 8；
 * large = `.m-btn.lg`：高 52，内边距 28，16sp。
 * 方向键聚焦：放大 1.1 + 外侧描边；Tonal / Outlined 变 inverseSurface，Filled 保持 primary。
 */
@Composable
fun BvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BvButtonStyle = BvButtonStyle.Filled,
    icon: ImageVector? = null,
    large: Boolean = false,
    height: Dp = if (large) 52.dp else 40.dp,
    horizontalPadding: Dp = if (large) 28.dp else 20.dp,
    textStyle: TextStyle = if (large) BvType.ButtonLarge else BvType.Button,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val baseContainer = containerColor ?: when (style) {
        BvButtonStyle.Filled -> scheme.primary
        BvButtonStyle.Tonal -> scheme.secondaryContainer
        BvButtonStyle.Outlined -> Color.Transparent
    }
    val baseContent = contentColor ?: when (style) {
        BvButtonStyle.Filled -> scheme.onPrimary
        BvButtonStyle.Tonal -> scheme.onSecondaryContainer
        BvButtonStyle.Outlined -> scheme.onSurfaceVariant
    }
    val keep = style == BvButtonStyle.Filled
    val container by focusContainerColor(focused, baseContainer, keep)
    val content by focusContentColor(focused, baseContent, keep)
    Surface(
        onClick = onClick,
        modifier = modifier
            .bvFocus()
            .height(height),
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = content,
        border = if (style == BvButtonStyle.Outlined && !showFocus(focused)) BorderStroke(1.dp, scheme.outline) else null,
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(PaddingValues(horizontal = horizontalPadding)),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = textStyle, maxLines = 1)
        }
    }
}

/** 原型 `.m-ib` 的样式：Standard（透明，onSurfaceVariant）、Tonal（surfaceContainerHighest）、Filled（primary）。 */
enum class BvIconButtonStyle { Standard, Tonal, Filled, SecondaryTonal }

/**
 * 原型 `.m-ib`：40dp 圆形，图标 24dp。方向键聚焦：放大 1.1 + 描边 + inverseSurface。
 */
@Composable
fun BvIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BvIconButtonStyle = BvIconButtonStyle.Standard,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    shape: Shape = CircleShape,
    focusCorner: Dp = 999.dp,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val baseContainer = containerColor ?: when (style) {
        BvIconButtonStyle.Standard -> Color.Transparent
        BvIconButtonStyle.Tonal -> scheme.surfaceContainerHighest
        BvIconButtonStyle.SecondaryTonal -> scheme.secondaryContainer
        BvIconButtonStyle.Filled -> scheme.primary
    }
    val baseContent = contentColor ?: when (style) {
        BvIconButtonStyle.Standard -> scheme.onSurfaceVariant
        BvIconButtonStyle.Tonal -> scheme.onSurface
        BvIconButtonStyle.SecondaryTonal -> scheme.onSecondaryContainer
        BvIconButtonStyle.Filled -> scheme.onPrimary
    }
    val container by focusContainerColor(focused, baseContainer)
    val content by focusContentColor(focused, baseContent)
    Surface(
        onClick = onClick,
        modifier = modifier
            .bvFocus(corner = focusCorner)
            .size(size),
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = content,
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
        }
    }
}
