package com.github.mytv.bv.ui.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 原型 `_src/4-data.js` 里 ICONS 的 Material Symbols 路径（24dp 网格），保证与原型一模一样。
 * 其余图标直接用 material-icons-extended。
 */
object BvIcons {
    val Shorts: ImageVector by lazy {
        icon(
            "Shorts",
            "M8 2h8a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V5a3 3 0 0 1 3-3zm0 2a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V5a1 1 0 0 0-1-1H8zm2 4.5l5 3.5-5 3.5v-7z"
        )
    }

    /** 选中态：实心竖屏 + 镂空播放三角 */
    val ShortsFilled: ImageVector by lazy {
        icon(
            "ShortsFilled",
            "M8 2h8a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V5a3 3 0 0 1 3-3zm2 6.5v7l5-3.5-5-3.5z"
        )
    }

    val Coin: ImageVector by lazy {
        icon(
            "Coin",
            "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 2a8 8 0 1 1 0 16 8 8 0 0 1 0-16zm-1 3v1.1A3 3 0 0 0 8.5 11c0 1.7 1.4 2.3 2.8 2.7 1.2.3 1.7.6 1.7 1.2 0 .6-.6 1-1.5 1-1 0-1.6-.5-1.7-1.2H8.2c.1 1.4 1.1 2.4 2.8 2.7V18h2v-.6c1.6-.3 2.6-1.3 2.6-2.6 0-1.8-1.5-2.4-2.9-2.8-1.1-.3-1.6-.5-1.6-1.1 0-.5.5-.9 1.3-.9.8 0 1.3.4 1.4 1h1.7c-.1-1.3-1-2.3-2.5-2.6V7h-2z"
        )
    }

    val Dynamic: ImageVector by lazy {
        icon(
            "Dynamic",
            "M20 8H4V6h16v2zm-2-6H6v2h12V2zm4 10v8c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2v-8c0-1.1.9-2 2-2h16c1.1 0 2 .9 2 2zm-6 4l-6-3.27v6.53L16 16z"
        )
    }

    val Heart: ImageVector by lazy {
        icon(
            "Heart",
            "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
        )
    }

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
}
