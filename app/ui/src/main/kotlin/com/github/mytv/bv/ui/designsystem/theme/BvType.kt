package com.github.mytv.bv.ui.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object BvType {
    val Display = TextStyle(fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).em) // 电视主标题
    val HeroTablet = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)    // 平板横幅标题
    val DetailTv = TextStyle(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)      // 电视详情标题
    val PageTitle = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)     // 手机/电视页标题「影视」「动态」「设置」
    val PageTitleLarge = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold) // 平板页标题
    val PaneTitle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)     // 右侧面板标题「历史记录」「外观与主题」
    val TitleLarge = TextStyle(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)    // 平板详情标题、电视刷刷标题
    val PanelTitle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)    // 电视设置右侧标题、播放器标题、我的页昵称
    val HeroPhone = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)     // 手机精选标题、弹幕设置标题
    val Title = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)              // 手机详情标题
    val Section = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)            // 区块标题「继续观看」
    val Body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val BodyBold = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val CardTitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)        // 卡片标题
    val Small = TextStyle(fontSize = 13.sp, lineHeight = 20.sp)
    val Label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val Meta = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)                                              // 卡片副信息
    val Eyebrow = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em) // 「今日精选 · 纪录」
    val Badge = TextStyle(fontSize = 10.5.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)        // 时长、评分角标
    val Button = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val ButtonLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
}
