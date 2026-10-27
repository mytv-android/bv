package com.github.mytv.bv.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.github.mytv.bv.player.impl.exo.ExoMediaPlayer

@OptIn(UnstableApi::class)
@Composable
fun BvVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    playerListener: VideoPlayerListener,
) {
    DisposableEffect(videoPlayer) {
        onDispose { videoPlayer.release() }
    }
    SideEffect {
        videoPlayer.setPlayerEventListener(playerListener)
    }

    when (videoPlayer) {
        is ExoMediaPlayer -> {
            AndroidView(
                modifier = modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = videoPlayer.mPlayer
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        useController = false
                    }
                },
                update = { view ->
                    if (view.player !== videoPlayer.mPlayer) view.player = videoPlayer.mPlayer
                },
            )
        }
    }
}
