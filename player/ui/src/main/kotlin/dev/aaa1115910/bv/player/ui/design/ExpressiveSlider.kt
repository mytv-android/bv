@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 第 9.4 节的 M3 Expressive 滑块外观（原型 `.m-sl`）：
 * 轨道高 16dp，已选段 primary（左圆角 8 右 3），未选段 secondaryContainer（左 3 右 8），
 * 手柄 4×36dp primary（圆角 2），手柄与两段轨道之间各留 6dp。
 */
@Composable
internal fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.secondaryContainer
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                Modifier
                    .size(width = 4.dp, height = 36.dp)
                    .background(primary, RoundedCornerShape(2.dp))
            )
        },
        track = { state ->
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                val x = state.coercedValueAsFraction * size.width
                val gap = 6.dp.toPx()
                val halfHandle = 2.dp.toPx()
                val activeEnd = x - halfHandle - gap
                if (activeEnd > 0f) {
                    drawSegment(0f, activeEnd, 8.dp, 3.dp, primary)
                }
                val inactiveStart = x + halfHandle + gap
                if (inactiveStart < size.width) {
                    drawSegment(inactiveStart, size.width, 3.dp, 8.dp, inactive)
                }
            }
        },
    )
}

private fun DrawScope.drawSegment(start: Float, end: Float, startCorner: Dp, endCorner: Dp, color: Color) {
    val s = CornerRadius(startCorner.toPx())
    val e = CornerRadius(endCorner.toPx())
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = start,
                top = 0f,
                right = end,
                bottom = size.height,
                topLeftCornerRadius = s,
                topRightCornerRadius = e,
                bottomRightCornerRadius = e,
                bottomLeftCornerRadius = s,
            )
        )
    }
    drawPath(path, color)
}
