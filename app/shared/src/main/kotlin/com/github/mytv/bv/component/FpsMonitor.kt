package com.github.mytv.bv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun FpsMonitor(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()
        FpsOverlay(modifier)
    }
}

/** 独立的重组作用域：FPS 变化只重组这里的 Text，不重新进入 content()。 */
@Composable
private fun BoxScope.FpsOverlay(modifier: Modifier) {
    var fps by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var fpsCount = 0
        var lastUpdate = 0L
        while (true) {
            withFrameMillis { ms ->
                fpsCount++
                if (fpsCount == 5) {
                    fps = (5000 / (ms - lastUpdate)).toInt()
                    lastUpdate = ms
                    fpsCount = 0
                }
            }
        }
    }

    Text(
        modifier = modifier
            .size(60.dp)
            .align(Alignment.TopStart),
        text = "Fps: $fps",
        color = Color.Red,
        style = MaterialTheme.typography.bodyMedium
    )
}