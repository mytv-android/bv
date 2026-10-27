package com.github.mytv.bv.player.entity

import android.content.Context
import com.github.mytv.bv.player.shared.R

enum class VideoPlayerClosedCaptionMenuItem(private val strRes: Int) {
    Switch(R.string.video_player_menu_subtitle_switch),
    Size(R.string.video_player_menu_subtitle_size),
    Opacity(R.string.video_player_menu_subtitle_background_opacity),
    Padding(R.string.video_player_menu_subtitle_bottom_padding);

    fun getDisplayName(context: Context) = context.getString(strRes)
}