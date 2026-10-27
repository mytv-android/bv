package com.github.mytv.bv.player.impl.exo

import android.content.Context
import com.github.mytv.bv.player.VideoPlayerOptions
import com.github.mytv.bv.player.factory.PlayerFactory

class ExoPlayerFactory : PlayerFactory<ExoMediaPlayer>() {
    override fun create(context: Context, options: VideoPlayerOptions): ExoMediaPlayer {
        return ExoMediaPlayer(context, options)
    }
}