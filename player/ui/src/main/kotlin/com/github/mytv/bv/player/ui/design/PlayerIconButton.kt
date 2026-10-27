@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package com.github.mytv.bv.player.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 播放器画面上的「裸图标」按钮（原型 `<span>${ic(...)}</span>`）：布局尺寸就是图标尺寸（默认 24dp），
 * 不加 48dp 的最小触摸区布局（Compose 会自动把小目标的触摸范围扩到最小触摸尺寸），
 * 这样控制层的位置与原型一致。方向键聚焦时按第 6.6 节放大 + 描边 + inverseSurface 底色。
 */
@Composable
internal fun PlainIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 24.dp,
) {
    val dpad = LocalPlayerDpad.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val showFocus = focused && dpad
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Color>()
    val baseColor = LocalContentColor.current
    val focusContainer = MaterialTheme.colorScheme.inverseSurface
    val container by animateColorAsState(
        if (showFocus) focusContainer else Color.Transparent, effects, label = "plainIconContainer"
    )
    val tint by animateColorAsState(
        when {
            showFocus -> MaterialTheme.colorScheme.inverseOnSurface
            enabled -> baseColor
            else -> baseColor.copy(alpha = 0.38f)
        },
        effects,
        label = "plainIconTint",
    )

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .bvFocus(dpad = dpad, corner = size)
            .drawBehind {
                if (container.alpha > 0f) drawCircle(container, radius = size.toPx() / 2)
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2 + 8.dp),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .size(size),
    )
}
