package com.github.mytv.bv.player.entity

import android.content.Context
import com.github.mytv.bv.player.shared.R

enum class DanmakuType(private val strRes: Int) {
    All(R.string.video_player_menu_danmaku_type_all),
    Top(R.string.video_player_menu_danmaku_type_top),
    Rolling(R.string.video_player_menu_danmaku_type_cross),
    Bottom(R.string.video_player_menu_danmaku_type_bottom);

    fun getDisplayName(context: Context) = context.getString(strRes)
}