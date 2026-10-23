@file:OptIn(ExperimentalMaterial3Api::class)

package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus

/**
 * 原型 `.m-sl`：M3 Expressive 滑块。轨道高 16，已选段 primary（左圆角 8 右 3），
 * 未选段 secondaryContainer（左 3 右 8），手柄 4×36 primary，手柄两侧各留 6dp 间隙。
 */
@Composable
fun BvSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.secondaryContainer
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .bvFocus(scale = 1.04f, corner = 8.dp),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interaction,
        thumb = {
            Box(
                Modifier
                    .size(width = 4.dp, height = 36.dp)
                    .background(primary, RoundedCornerShape(2.dp))
            )
        },
        track = { state ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    // 进度在 Draw 阶段读取；Path 按尺寸缓存，拖动时只 rewind 不重新分配
                    .drawWithCache {
                        val gap = 6.dp.toPx()
                        val big = CornerRadius(8.dp.toPx())
                        val small = CornerRadius(3.dp.toPx())
                        val activePath = Path()
                        val inactivePath = Path()
                        onDrawBehind {
                            val range = state.valueRange
                            val fraction = if (range.endInclusive == range.start) 0f
                            else ((state.value - range.start) / (range.endInclusive - range.start))
                                .coerceIn(0f, 1f)
                            val h = size.height
                            val x = size.width * fraction
                            val activeEnd = (x - gap).coerceAtLeast(0f)
                            if (activeEnd > 0f) {
                                activePath.rewind()
                                activePath.addRoundRect(
                                    RoundRect(
                                        left = 0f, top = 0f, right = activeEnd, bottom = h,
                                        topLeftCornerRadius = big, bottomLeftCornerRadius = big,
                                        topRightCornerRadius = small, bottomRightCornerRadius = small,
                                    )
                                )
                                drawPath(activePath, primary)
                            }
                            val inactiveStart = (x + gap).coerceAtMost(size.width)
                            if (inactiveStart < size.width) {
                                inactivePath.rewind()
                                inactivePath.addRoundRect(
                                    RoundRect(
                                        left = inactiveStart, top = 0f, right = size.width, bottom = h,
                                        topLeftCornerRadius = small, bottomLeftCornerRadius = small,
                                        topRightCornerRadius = big, bottomRightCornerRadius = big,
                                    )
                                )
                                drawPath(inactivePath, inactive)
                            }
                        }
                    }
            )
        },
    )
}

/** M3 Switch + 方向键焦点效果。 */
@Composable
fun BvSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.bvFocus(),
        enabled = enabled,
    )
}
