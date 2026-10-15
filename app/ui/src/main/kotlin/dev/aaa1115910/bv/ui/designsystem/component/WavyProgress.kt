@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * 播放进度：播放中 LinearWavyProgressIndicator（高 12dp），暂停时拉直为普通进度条，Crossfade(defaultEffectsSpec)。
 * onSeek != null 时在上面叠一个透明 Slider 处理拖动。
 */
@Composable
fun WavyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = Color.White.copy(alpha = .35f),
    onSeek: ((Float) -> Unit)? = null,
    onSeekFinished: (() -> Unit)? = null,
) {
    val p = progress.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(12.dp), contentAlignment = Alignment.Center) {
        Crossfade(
            targetState = playing,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "wavy",
        ) { wavy ->
            if (wavy) {
                LinearWavyProgressIndicator(
                    progress = { p },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = color,
                    trackColor = trackColor,
                )
            } else {
                Box(Modifier.fillMaxWidth().height(12.dp), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = color,
                        trackColor = trackColor,
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                    )
                }
            }
        }
        if (onSeek != null) {
            Slider(
                value = p,
                onValueChange = onSeek,
                onValueChangeFinished = onSeekFinished,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}
