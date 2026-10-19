package dev.aaa1115910.bv.ui.designsystem.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.aaa1115910.bv.ui.designsystem.adaptive.BvInputMode
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import kotlin.math.min

/**
 * 外侧 3dp 描边 + 光晕，带淡入淡出动画。
 * 动画值只在 graphicsLayer / draw 阶段读取，避免光晕每帧重组整棵内容。
 */
fun Modifier.focusRing(visible: Boolean, corner: Dp, glow: Dp = 14.dp): Modifier =
    focusRing(visible, { corner }, glow)

/** [corner] 以 lambda 提供，用于随焦点动画变化的圆角（只在 layer / draw 阶段读取）。 */
fun Modifier.focusRing(visible: Boolean, corner: () -> Dp, glow: Dp = 14.dp): Modifier = composed {
    val alphaState = animateFloatAsState(
        if (visible) 1f else 0f,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "focusRing",
    )
    val color = MaterialTheme.colorScheme.primary
    this
        .graphicsLayer {
            shadowElevation = glow.toPx() * alphaState.value
            shape = RoundedCornerShape(corner())
            clip = false
            ambientShadowColor = color
            spotShadowColor = color
        }
        .drawWithContent {
            drawContent()
            val alpha = alphaState.value
            if (alpha > 0f) {
                val c = corner()
                val w = 3.dp.toPx()
                val r = min(c.toPx() + w / 2, (min(size.width, size.height) + w) / 2)
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(-w / 2, -w / 2),
                    size = Size(size.width + w, size.height + w),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(w),
                )
            }
        }
}

/**
 * 通用焦点效果。给 M3 自带焦点的组件（Button、IconButton、Surface(onClick)…）用时 addFocusable = false；
 * 给普通 Box 用时 addFocusable = true。onFocused 用于「聚焦即选中」。
 */
fun Modifier.bvFocus(
    scale: Float = 1.1f,
    corner: Dp = 999.dp,
    glow: Dp = 14.dp,
    addFocusable: Boolean = false,
    onFocused: ((Boolean) -> Unit)? = null,
): Modifier = composed {
    val profile = LocalDeviceProfile.current
    var focused by remember { mutableStateOf(false) }
    val show = focused && profile.input == BvInputMode.DPad
    val s by animateFloatAsState(if (show) scale else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "bvFocusScale")
    this
        .zIndex(if (show) 1f else 0f)
        .graphicsLayer { scaleX = s; scaleY = s }
        .focusRing(show, corner, glow)
        .onFocusChanged { focused = it.hasFocus; onFocused?.invoke(it.hasFocus) }
        .then(if (addFocusable) Modifier.focusable() else Modifier)
}

/** 卡片焦点状态：圆角以 [State] 暴露，调用方在 layer / draw 阶段读取，避免弹簧动画每帧重组整张卡。 */
@Stable
class CardFocus internal constructor(
    val show: Boolean,
    val corner: State<Dp>,
)

/** 卡片用：给出是否显示焦点效果、以及封面圆角（16→22dp 弹簧）。 */
@Composable
fun rememberCardFocus(focused: Boolean): CardFocus {
    val show = focused && LocalDeviceProfile.current.input == BvInputMode.DPad
    val corner = animateDpAsState(
        if (show) 22.dp else 16.dp,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "cardCorner",
    )
    return remember(show, corner) { CardFocus(show, corner) }
}

/** 焦点效果是否应当显示：只在方向键 / 键盘输入模式下显示。 */
@Composable
fun showFocus(focused: Boolean): Boolean = focused && LocalDeviceProfile.current.input == BvInputMode.DPad

/** 第 6.6 节聚焦变色：方向键聚焦时容器换成 inverseSurface。keep = true 时保持原色（filled primary 按钮）。 */
@Composable
fun focusContainerColor(focused: Boolean, normal: Color, keep: Boolean = false): State<Color> {
    val target = if (showFocus(focused) && !keep) MaterialTheme.colorScheme.inverseSurface else normal
    return animateColorAsState(target, MaterialTheme.motionScheme.fastEffectsSpec(), label = "focusContainer")
}

/** 第 6.6 节聚焦变色：方向键聚焦时内容换成 inverseOnSurface。 */
@Composable
fun focusContentColor(focused: Boolean, normal: Color, keep: Boolean = false): State<Color> {
    val target = if (showFocus(focused) && !keep) MaterialTheme.colorScheme.inverseOnSurface else normal
    return animateColorAsState(target, MaterialTheme.motionScheme.fastEffectsSpec(), label = "focusContent")
}
