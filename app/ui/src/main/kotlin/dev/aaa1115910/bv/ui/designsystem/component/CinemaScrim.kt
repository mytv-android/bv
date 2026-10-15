package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val ScrimBottom = Brush.verticalGradient(
    0.6f to Color.Transparent,
    1f to Color.Black.copy(alpha = .78f),
)
private val ScrimTop = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = .45f),
    0.22f to Color.Transparent,
)

/** 原型 `.m-scrim-b`：下方 黑 α0.78 → 40% 高处透明；上方 黑 α0.45 → 22% 高处透明。铺满父容器。 */
@Composable
fun CinemaScrim(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(ScrimBottom)
                drawRect(ScrimTop)
            }
    )
}
