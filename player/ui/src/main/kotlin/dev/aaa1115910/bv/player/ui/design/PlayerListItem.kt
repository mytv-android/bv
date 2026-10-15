@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ListItemShape = RoundedCornerShape(16.dp)

/**
 * 第 7.15 节 `BvListItem`（原型 `.m-li`）的外壳：
 * `Row(spacedBy(14.dp), CenterVertically)`，内边距 水平 16 垂直 10，圆角 16。
 * 选中态：背景 secondaryContainer、内容色 onSecondaryContainer。
 * 方向键聚焦：放大 1.10 + 外侧描边，背景 inverseSurface、内容色 inverseOnSurface（第 6.6 节）。
 *
 * [onFocused] 用于「聚焦即选中」（电视菜单的分类列）。
 */
@Composable
internal fun PlayerListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(14.dp),
    onFocused: ((Boolean) -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val dpad = LocalPlayerDpad.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val showFocus = focused && dpad
    val colorScheme = MaterialTheme.colorScheme
    val baseContent = LocalContentColor.current
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Color>()

    val container by animateColorAsState(
        when {
            showFocus -> colorScheme.inverseSurface
            selected -> colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        effects,
        label = "listItemContainer",
    )
    val contentColor by animateColorAsState(
        when {
            showFocus -> colorScheme.inverseOnSurface
            selected -> colorScheme.onSecondaryContainer
            else -> baseContent
        },
        effects,
        label = "listItemContent",
    )

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .bvFocus(dpad = dpad, corner = 16.dp, onFocused = onFocused)
                .clip(ListItemShape)
                .background(container, ListItemShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
