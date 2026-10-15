@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.design

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * 第 6.3 节 BvType 中播放器用到的条目（数值照抄）。player:ui 不能依赖 app:ui，所以复制一份。
 */
internal object PlayerType {
    val PanelTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)    // 电视播放器标题
    val HeroPhone = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)     // 侧边面板标题「弹幕设置」
    val Body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val BodyBold = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val Small = TextStyle(fontSize = 13.sp, lineHeight = 20.sp)
    val Label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val Meta = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
    val Eyebrow = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em)

    /** 原型 `.num`：等宽数字（时间、数值）。 */
    const val TabularNum = "tnum"
}
