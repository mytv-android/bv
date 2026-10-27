@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package com.github.mytv.bv.player.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 播放器画面上的小标签：内边距 3×10（原型 `padding:3px 10px`）。 */
internal val CompactChipPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp)

/** 画面上小标签的文字：13sp Medium（原型 `.m-chip` 的 font）。 */
internal val CompactChipText = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

/**
 * 第 7.5 节 `BvChip`（原型 `.m-chip`）。
 * - 未选中：圆角 10dp，1dp 内描边 outlineVariant，文字 onSurfaceVariant。
 * - 选中：圆角变为全圆（defaultSpatialSpec），背景 secondaryContainer，文字 onSecondaryContainer，无描边。
 * - 方向键操作时：bvFocus()，聚焦时背景 inverseSurface、文字 inverseOnSurface。
 *
 * 播放器画面上的白色描边小标签通过 [unselectedContentColor] / [unselectedBorderColor] / [contentPadding] 定制。
 */
@Composable
internal fun PlayerChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    textStyle: TextStyle = PlayerType.Small.copy(fontWeight = FontWeight.Medium),
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedBorderColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val dpad = LocalPlayerDpad.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val showFocus = focused && dpad
    val colorScheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Color>()

    val container by animateColorAsState(
        when {
            showFocus -> colorScheme.inverseSurface
            selected -> colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        effects,
        label = "chipContainer",
    )
    val content by animateColorAsState(
        when {
            showFocus -> colorScheme.inverseOnSurface
            selected -> colorScheme.onSecondaryContainer
            else -> unselectedContentColor
        },
        effects,
        label = "chipContent",
    )
    val shape = animatedPillShape(from = 10.dp, pill = selected, label = "chipShape")

    Box(
        modifier = modifier
            .bvFocus(dpad = dpad, corner = 999.dp)
            .clip(shape)
            .background(container, shape)
            .then(
                if (selected || showFocus) Modifier
                else Modifier.border(1.dp, unselectedBorderColor, shape)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(contentPadding),
    ) {
        Text(text = text, style = textStyle, color = content, maxLines = 1)
    }
}
