@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.github.mytv.bv.ui.designsystem.nav

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mytv.bv.ui.navigation.TopLevel

/**
 * 原型 `.m-nbar`：ShortNavigationBar（surfaceContainer），5 项。图标 24dp 在 56×32 全圆角指示器里，
 * 选中指示器 secondaryContainer；标签 11sp，选中 onSurface Bold。
 * overVideo = true（刷刷页）：黑 α0.72，未选中白 α0.7，选中标签白色。
 */
@Composable
fun PhoneNavBar(
    current: TopLevel,
    onSelect: (TopLevel) -> Unit,
    modifier: Modifier = Modifier,
    overVideo: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = if (overVideo) {
        ShortNavigationBarItemDefaults.colors(
            selectedIconColor = scheme.onSecondaryContainer,
            selectedTextColor = Color.White,
            selectedIndicatorColor = scheme.secondaryContainer,
            unselectedIconColor = Color.White.copy(alpha = .7f),
            unselectedTextColor = Color.White.copy(alpha = .7f),
        )
    } else {
        ShortNavigationBarItemDefaults.colors(
            selectedIconColor = scheme.onSecondaryContainer,
            selectedTextColor = scheme.onSurface,
            selectedIndicatorColor = scheme.secondaryContainer,
            unselectedIconColor = scheme.onSurfaceVariant,
            unselectedTextColor = scheme.onSurfaceVariant,
        )
    }
    ShortNavigationBar(
        modifier = modifier,
        containerColor = if (overVideo) Color.Black.copy(alpha = .72f) else scheme.surfaceContainer,
    ) {
        TopLevel.entries.forEach { item ->
            val selected = item == current
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = {
                    Icon(
                        if (selected) item.iconSelected() else item.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        stringResource(item.label),
                        style = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                },
                colors = colors,
            )
        }
    }
}
