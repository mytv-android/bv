package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * 原型 `.m-backdrop`（电视全幅背景）：右上角，宽 74%、高 76%（视频详情 70%）。
 * 叠横向（surface → surface α0.7 @30% → 透明 @62%）与纵向（底部 2% surface → 48% 透明）两层渐变。
 */
@Composable
fun TvBackdrop(cover: String?, modifier: Modifier = Modifier, heightFraction: Float = 0.76f) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.74f)
                .fillMaxHeight(heightFraction)
        ) {
            Crossfade(
                targetState = cover,
                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                label = "tvBackdrop",
            ) { url ->
                if (!url.isNullOrEmpty()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to surface,
                            0.3f to surface.copy(alpha = 0.7f),
                            0.62f to Color.Transparent,
                        )
                    )
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.52f to Color.Transparent,
                            0.98f to surface,
                        )
                    )
            )
        }
    }
}
