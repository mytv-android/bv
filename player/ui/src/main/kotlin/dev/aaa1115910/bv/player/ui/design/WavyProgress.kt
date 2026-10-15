@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.design

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WavyTrackColor = Color.White.copy(alpha = 0.35f)

/**
 * 第 7.14 节 `WavyProgress`：播放中 `LinearWavyProgressIndicator`（高 12dp），
 * 暂停时改用普通 `LinearProgressIndicator`（拉直），切换用 `Crossfade(defaultEffectsSpec)`。
 * 进度色默认 primary（详情内嵌播放器传 inversePrimary），轨道色 White α0.35。
 */
@Composable
internal fun WavyProgress(
    progress: () -> Float,
    waving: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Crossfade(
        targetState = waving,
        modifier = modifier.fillMaxWidth(),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "wavyProgress",
    ) { wavy ->
        if (wavy) {
            LinearWavyProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = color,
                trackColor = WavyTrackColor,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = WavyTrackColor,
                )
            }
        }
    }
}

/**
 * 可拖动的 [WavyProgress]：在其上叠一个透明 `Slider` 处理拖动和点按（第 7.14 节）。
 * 布局高度仍是 12dp，透明 Slider 用 requiredHeight 向上下溢出，扩大触摸区域。
 *
 * @param progress 当前播放进度 0..1
 * @param onDrag 拖动中回调当前比例；松手后回调 null
 * @param onSeek 松手（或点按）时回调目标比例
 */
@Composable
internal fun DraggableWavyProgress(
    progress: Float,
    waving: Boolean,
    onDrag: (Float?) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val shown = dragFraction ?: progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        WavyProgress(
            progress = { dragFraction ?: progress.coerceIn(0f, 1f) },
            waving = waving && dragFraction == null,
            color = color,
        )
        Slider(
            value = shown,
            onValueChange = {
                dragFraction = it
                currentOnDrag(it)
            },
            onValueChangeFinished = {
                dragFraction?.let { currentOnSeek(it) }
                dragFraction = null
                currentOnDrag(null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(36.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            thumb = {},
            track = { Spacer(Modifier.fillMaxWidth().height(12.dp)) },
        )
    }
}
