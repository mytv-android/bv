package com.github.mytv.bv.player.entity

import android.content.Context
import com.github.mytv.bv.player.shared.R

enum class VideoPlayerOthersMenuItem(private val strRes: Int) {
    PlayMode(R.string.video_player_menu_others_play_mode);

    fun getDisplayName(context: Context) = context.getString(strRes)
}