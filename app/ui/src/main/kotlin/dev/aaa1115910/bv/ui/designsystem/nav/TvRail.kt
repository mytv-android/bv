package dev.aaa1115910.bv.ui.designsystem.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
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
import kotlinx.coroutines.delay

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
    autoHide: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(false) }
    // 导轨聚焦期间页面被切走（例如确认键切换）时，把焦点同步到新页面对应的项。
    // 延迟是为了给新页面的初始焦点请求让路，避免导轨把焦点抢回去导致自动隐藏不生效；
    // 复查 wasFocused：新页面拿到焦点后就不再抢。
    LaunchedEffect(current) {
        if (!wasFocused) return@LaunchedEffect
        delay(250L)
        if (wasFocused) runCatching { selectedRequester.requestFocus() }
    }
    // 全屏页面播放中自动隐藏导航轨；焦点进入或离开自动隐藏页面时立即恢复显示。
    LaunchedEffect(autoHide, wasFocused) {
        if (!autoHide || wasFocused) {
            hidden = false
        } else {
            delay(3_000L)
            hidden = true
        }
    }
    // 淡出结束后把整条导轨折叠成 0 尺寸：alpha(0) 依旧参与触摸命中，
    // 不折叠的话全屏页面左缘的手势会被隐藏的导轨项当成点击。
    LaunchedEffect(hidden) {
        if (hidden) {
            delay(700L)
            collapsed = true
        } else {
            collapsed = false
        }
    }
    val railAlpha = animateFloatAsState(
        if (hidden) 0f else 1f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tvRailAlpha",
    )
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
            .layout { measurable, constraints ->
                if (collapsed) {
                    measurable.measure(Constraints(0, 0, 0, 0))
                    layout(0, 0) {}
                } else {
                    val widthPx = constraints.constrainWidth(width.roundToPx().coerceAtLeast(0))
                    val placeable = measurable.measure(constraints.copy(minWidth = widthPx, maxWidth = widthPx))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
            .fillMaxHeight()
            .graphicsLayer { alpha = railAlpha.value }
            .drawWithCache {
                val shadowWidth = 48.dp.toPx()
                val shadow = Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = .7f), Color.Transparent),
                    startX = size.width,
                    endX = size.width + shadowWidth,
                )
                onDrawBehind {
                    if (open) drawRect(
                        shadow,
                        topLeft = Offset(size.width, 0f),
                        size = Size(shadowWidth, size.height),
                    )
                    drawRect(background)
                }
            }
            .onFocusChanged {
                val focused = it.hasFocus
                // 从内容区进入导航轨时，焦点固定到当前页面对应的项。
                // 不能用 focusRestorer：它会记住上次停留的项（常见是「搜索」），切换页面后焦点会落回搜索，确认键就打开了搜索页。
                if (focused && !wasFocused) runCatching { selectedRequester.requestFocus() }
                wasFocused = focused
                open = focused
                onFocusChanged(focused)
            }
            .focusGroup()
            // 弹簧会过冲，内边距不能为负
            .layout { measurable, constraints ->
                val horizontal = hPad.roundToPx().coerceAtLeast(0)
                val vertical = 27.dp.roundToPx()
                val placeable = measurable.measure(constraints.offset(-horizontal * 2, -vertical * 2))
                layout(
                    constraints.constrainWidth(placeable.width + horizontal * 2),
                    constraints.constrainHeight(placeable.height + vertical * 2),
                ) { placeable.placeRelative(horizontal, vertical) }
            },
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
