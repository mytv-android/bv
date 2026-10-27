package com.github.mytv.bv.player.factory

import android.content.Context
import com.github.mytv.bv.player.AbstractVideoPlayer
import com.github.mytv.bv.player.VideoPlayerOptions

abstract class PlayerFactory<T : AbstractVideoPlayer> {
    abstract fun create(context: Context, options: VideoPlayerOptions): T
}