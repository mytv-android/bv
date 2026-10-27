package com.github.mytv.bv.player.entity

import android.content.Context
import com.github.mytv.bv.player.shared.R

enum class VideoPlayerPictureMenuItem(private val strRes: Int) {
    Resolution(R.string.video_player_menu_picture_resolution),
    Codec(R.string.video_player_menu_picture_codec),
    AspectRatio(R.string.video_player_menu_picture_aspect_ratio),
    PlaySpeed(R.string.video_player_menu_picture_play_speed),
    Audio(R.string.video_player_menu_picture_audio);

    fun getDisplayName(context: Context) = context.getString(strRes)
}