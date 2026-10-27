package com.github.mytv.bv.player.entity

import android.content.Context
import com.github.mytv.bv.player.shared.R

enum class VideoPlayerDanmakuMenuItem(private val strRes: Int) {
    Switch(R.string.video_player_menu_danmaku_switch),
    Size(R.string.video_player_menu_danmaku_size),
    Opacity(R.string.video_player_menu_danmaku_opacity),
    Area(R.string.video_player_menu_danmaku_area),
    Mask(R.string.video_player_menu_danmaku_mask);

    fun getDisplayName(context: Context) = context.getString(strRes)
}