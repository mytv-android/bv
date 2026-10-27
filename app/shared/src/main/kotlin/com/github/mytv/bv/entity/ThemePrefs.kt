package com.github.mytv.bv.entity

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

enum class ThemeSource { Wallpaper, Custom, Content }      // 系统壁纸 / 自选种子色 / 跟随内容
enum class DarkMode { System, Light, Dark, PureBlack }     // 跟随系统 / 浅色 / 深色 / 影院纯黑

object BvSeeds {
    val Pink = Color(0xFFFB7299)       // B 站粉（默认）
    val Sky = Color(0xFF23ADE5)        // 天蓝
    val Amber = Color(0xFFF2A53A)      // 琥珀
    val Teal = Color(0xFF1FA88C)       // 青绿
    val Violet = Color(0xFF8C6FF7)     // 紫罗兰
    val Vermilion = Color(0xFFE5484D)  // 朱红
    val presets = listOf(Pink, Sky, Amber, Teal, Violet, Vermilion)
}

data class ThemePrefs(
    val source: ThemeSource = ThemeSource.Custom,
    val seed: Color = BvSeeds.Pink,
    val contentSeed: Color? = null,                 // 「跟随内容」时使用：最近一次播放视频的封面色
    val style: PaletteStyle = PaletteStyle.Vibrant,
    val contrast: Double = 0.0,                     // 0.0 标准 / 0.5 中 / 1.0 高
    val darkMode: DarkMode = DarkMode.System,
    val coverTheme: Boolean = true,                 // 详情页使用封面颜色
    val reduceMotion: Boolean = false,              // 减少动效
)
