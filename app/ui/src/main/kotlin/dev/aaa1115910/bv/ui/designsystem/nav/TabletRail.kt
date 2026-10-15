@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.aaa1115910.bv.ui.designsystem.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalWideNavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.focus.focusContainerColor
import dev.aaa1115910.bv.ui.designsystem.focus.focusContentColor
import dev.aaa1115910.bv.ui.navigation.TopLevel
import kotlinx.coroutines.launch

/**
 * 原型 `.m-rail`：收起 96dp（菜单按钮 + 56dp 搜索 FAB + 5 个入口 + 底部设置）；
 * 展开 220dp（`.m-rail.wide`，扩展型 FAB + 横排 52dp 高的入口）。收起时点菜单按钮弹出模态展开导航轨。
 * current == null 时不选中任何项（例如搜索页）。
 */
@Composable
fun TabletRail(
    current: TopLevel?,
    expanded: Boolean,
    onSelect: (TopLevel) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val modalState = rememberWideNavigationRailState(WideNavigationRailValue.Collapsed)
    if (expanded) {
        RailExpandedContent(current, onSelect, onSearch, onSettings, modifier.background(MaterialTheme.colorScheme.surface))
    } else {
        Column(
            modifier
                .width(96.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BvIconButton(
                icon = Icons.Rounded.Menu,
                contentDescription = stringResource(R.string.nav_menu),
                onClick = { scope.launch { modalState.expand() } },
            )
            Surface(
                onClick = onSearch,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .bvFocus(corner = 16.dp)
                    .size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.nav_search))
                }
            }
            TopLevel.entries.forEach { item ->
                CollapsedRailItem(
                    icon = if (item == current) item.iconSelected() else item.icon(),
                    label = stringResource(item.label),
                    selected = item == current,
                    onClick = { onSelect(item) },
                )
            }
            Spacer(Modifier.weight(1f))
            CollapsedRailItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.nav_settings),
                selected = false,
                onClick = onSettings,
            )
        }
        ModalWideNavigationRail(
            state = modalState,
            hideOnCollapse = true,
        ) {
            RailExpandedContent(
                current = current,
                onSelect = { scope.launch { modalState.collapse() }; onSelect(it) },
                onSearch = { scope.launch { modalState.collapse() }; onSearch() },
                onSettings = { scope.launch { modalState.collapse() }; onSettings() },
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun RailExpandedContent(
    current: TopLevel?,
    onSelect: (TopLevel) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .width(220.dp)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            onClick = onSearch,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 16.dp)
                .bvFocus(corner = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Row(
                Modifier.padding(start = 18.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Text(
                    stringResource(R.string.nav_search),
                    style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
        TopLevel.entries.forEach { item ->
            ExpandedRailItem(
                icon = if (item == current) item.iconSelected() else item.icon(),
                label = stringResource(item.label),
                selected = item == current,
                onClick = { onSelect(item) },
            )
        }
        Spacer(Modifier.weight(1f))
        ExpandedRailItem(
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.nav_settings),
            selected = false,
            onClick = onSettings,
        )
    }
}

@Composable
private fun ColumnScope.CollapsedRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val indicator by animateColorAsState(
        if (selected) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "railIndicator"
    )
    val indicatorColor by focusContainerColor(focused, indicator)
    val iconColor by focusContentColor(focused, if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant)
    Surface(
        onClick = onClick,
        modifier = Modifier.width(80.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        interactionSource = interaction,
    ) {
        Column(
            Modifier.padding(top = 2.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .bvFocus()
                    .size(width = 56.dp, height = 32.dp)
                    .background(indicatorColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Text(
                label,
                style = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpandedRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val base by animateColorAsState(
        if (selected) scheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "railItem"
    )
    val container by focusContainerColor(focused, base)
    val content by focusContentColor(focused, if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .bvFocus()
            .height(52.dp),
        shape = CircleShape,
        color = container,
        contentColor = content,
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                label,
                style = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
            )
        }
    }
}
