package dev.aaa1115910.bv.ui.designsystem.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.focus.focusContainerColor
import dev.aaa1115910.bv.ui.designsystem.focus.focusContentColor
import dev.aaa1115910.bv.ui.navigation.TopLevel

/** 电视导航轨里「搜索」也是一个可选中项。 */
sealed interface TvRailTarget {
    data object Search : TvRailTarget
    data class Page(val top: TopLevel) : TvRailTarget
}

/**
 * 原型 `.m-tvrail`：收起 76dp 只有图标；焦点进入时展开为 196dp（defaultSpatialSpec），
 * 背景 surfaceContainer + 向右 48dp 的黑色阴影。按确认键才切换页面。
 * selectedRequester 挂在当前选中项上，外壳用它把焦点移回导航轨。
 */
@Composable
fun TvRail(
    current: TvRailTarget?,
    avatar: String?,
    userName: String,
    onSelect: (TopLevel) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    selectedRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.Dp>()
    val width by animateDpAsState(if (open) 196.dp else 76.dp, spatial, label = "tvRailWidth")
    val hPad by animateDpAsState(if (open) 14.dp else 0.dp, spatial, label = "tvRailPad")
    val background by animateColorAsState(
        if (open) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tvRailBg"
    )
    Column(
        modifier
            .width(width)
            .fillMaxHeight()
            .drawBehind {
                if (open) {
                    // box-shadow: 24px 0 48px -24px rgb(0 0 0 / .7)
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = .7f), Color.Transparent),
                            startX = size.width,
                            endX = size.width + 48.dp.toPx(),
                        ),
                        topLeft = Offset(size.width, 0f),
                        size = Size(48.dp.toPx(), size.height),
                    )
                }
                drawRect(background)
            }
            .onFocusChanged { open = it.hasFocus; onFocusChanged(it.hasFocus) }
            .focusRestorer(selectedRequester)
            .focusGroup()
            .padding(vertical = 27.dp, horizontal = hPad),
        horizontalAlignment = if (open) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RailEntry(
            icon = null,
            label = userName,
            selected = false,
            open = open,
            onClick = { onSelect(TopLevel.Me) },
            leading = { BvAvatar(url = avatar, size = 36.dp, fallbackText = userName) },
        )
        Spacer(Modifier.height(10.dp))
        RailEntry(
            icon = Icons.Rounded.Search,
            label = stringResource(R.string.nav_search),
            selected = current == TvRailTarget.Search,
            open = open,
            onClick = onSearch,
            modifier = if (current == TvRailTarget.Search) Modifier.focusRequester(selectedRequester) else Modifier,
        )
        TopLevel.entries.forEach { item ->
            val selected = (current as? TvRailTarget.Page)?.top == item
            RailEntry(
                icon = if (selected) item.iconSelected() else item.icon(),
                label = stringResource(item.label),
                selected = selected,
                open = open,
                onClick = { onSelect(item) },
                modifier = if (selected) Modifier.focusRequester(selectedRequester) else Modifier,
            )
        }
        Spacer(Modifier.weight(1f))
        RailEntry(
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.nav_settings),
            selected = false,
            open = open,
            onClick = onSettings,
        )
    }
}

@Composable
private fun RailEntry(
    icon: ImageVector?,
    label: String,
    selected: Boolean,
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val base = when {
        leading != null -> Color.Transparent
        selected -> scheme.secondaryContainer
        else -> Color.Transparent
    }
    val container by focusContainerColor(focused, base)
    val content by focusContentColor(focused, if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant)
    Surface(
        onClick = onClick,
        modifier = modifier
            .bvFocus()
            .height(44.dp)
            .then(if (open) Modifier.fillMaxWidth() else Modifier.width(44.dp)),
        shape = CircleShape,
        color = container,
        contentColor = content,
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(horizontal = if (open && leading == null) 14.dp else if (open) 4.dp else 0.dp),
            horizontalArrangement = if (open) Arrangement.spacedBy(14.dp) else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { leading() }
            } else if (icon != null) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
            AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    label,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
            }
        }
    }
}
