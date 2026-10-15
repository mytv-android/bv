package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.focus.focusContainerColor
import dev.aaa1115910.bv.ui.designsystem.focus.focusContentColor
import dev.aaa1115910.bv.ui.designsystem.theme.BvType

/**
 * 原型 `.m-li`：Row(spacedBy 14)，内边距 16×10，圆角 16。左侧 40dp 图标容器（圆角 12 或传入形状），
 * 标题 14sp SemiBold + 副标题 12sp onSurfaceVariant。选中：secondaryContainer。
 * 方向键聚焦：放大 1.1 + 描边 + inverseSurface；onFocused 可实现「聚焦即切换」。
 */
@Composable
fun BvListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    iconContentColor: Color = MaterialTheme.colorScheme.onSurface,
    iconShape: Shape = RoundedCornerShape(12.dp),
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onFocused: ((Boolean) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    titleStyle: TextStyle = BvType.BodyBold,
    centered: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val baseContainer by animateColorAsState(
        if (selected) scheme.secondaryContainer else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "liContainer"
    )
    val baseContent = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    val container by focusContainerColor(focused, baseContainer)
    val content by focusContentColor(focused, baseContent)
    val shape = RoundedCornerShape(16.dp)
    val body: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
            } else if (icon != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(iconShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconContentColor)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    style = titleStyle,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        subtitle,
                        style = BvType.Label.copy(fontWeight = FontWeight.Normal),
                        color = if (selected) LocalContentColor.current.copy(alpha = .8f) else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) trailing()
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.bvFocus(corner = 16.dp, onFocused = onFocused),
            shape = shape,
            color = container,
            contentColor = content,
            interactionSource = interaction,
        ) { body() }
    } else {
        Surface(modifier = modifier, shape = shape, color = container, contentColor = content) {
            CompositionLocalProvider(LocalContentColor provides content) { body() }
        }
    }
}
