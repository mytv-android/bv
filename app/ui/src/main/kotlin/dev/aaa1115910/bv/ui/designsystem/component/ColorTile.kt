package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus

enum class TileRole { Primary, Secondary, Tertiary }

/**
 * 原型 `tile()`：「我的」页彩色入口卡。内边距 14，圆角 24；图标容器 40dp 圆角 14 背景 {role}；
 * 标题 15sp Bold，副标题 12sp α0.8。方向键聚焦：放大 1.05 + 描边 + 光晕 16。
 */
@Composable
fun ColorTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    role: TileRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content, accent, onAccent) = when (role) {
        TileRole.Primary -> listOf(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary, scheme.onPrimary)
        TileRole.Secondary -> listOf(scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary, scheme.onSecondary)
        TileRole.Tertiary -> listOf(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary, scheme.onTertiary)
    }
    Surface(
        onClick = onClick,
        modifier = modifier.bvFocus(scale = 1.05f, corner = 24.dp, glow = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = content,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = onAccent, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    title,
                    style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    modifier = Modifier.alpha(.8f),
                    style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 热搜名次：22dp 方块，圆角 8，12sp ExtraBold 等宽。1 primary / 2 tertiary / 3 secondary，其后无背景。 */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (rank) {
        1 -> scheme.primary to scheme.onPrimary
        2 -> scheme.tertiary to scheme.onTertiary
        3 -> scheme.secondary to scheme.onSecondary
        else -> Color.Transparent to scheme.onSurfaceVariant
    }
    Box(
        modifier
            .size(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$rank",
            style = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
            color = fg,
        )
    }
}
