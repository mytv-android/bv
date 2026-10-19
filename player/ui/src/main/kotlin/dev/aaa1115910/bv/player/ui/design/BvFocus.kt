@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.min

// 第 6.6 节焦点系统的 player:ui 内部版本。
// 与 app:ui 的区别只有一处：不读 LocalDeviceProfile（在 app:ui 里，player:ui 依赖不到），
// 改为由调用方传入 dpad —— 电视，或当前输入模式是键盘（平板接蓝牙键盘）时为 true。

/** 播放器内「是否处于方向键操作」。由 BvPlayerScreen 根据 isTv 与输入模式提供。 */
internal val LocalPlayerDpad = compositionLocalOf { false }

/** 「减少动效」：为 true 时形状变形直接跳到终态（第 6.9 节）。由 BvPlayerScreen 提供。 */
internal val LocalPlayerReduceMotion = compositionLocalOf { false }

/** 电视，或输入模式为键盘时为 true。 */
@Composable
internal fun rememberDpadMode(isTv: Boolean): Boolean =
    isTv || LocalInputModeManager.current.inputMode == InputMode.Keyboard

/**
 * 外侧 3dp 描边 + 光晕，带淡入淡出动画。
 * 动画值只在 graphicsLayer / draw 阶段读取，避免光晕每帧重组整棵内容。
 */
internal fun Modifier.focusRing(visible: Boolean, corner: Dp, glow: Dp = 14.dp): Modifier = composed {
    val alphaState = animateFloatAsState(
        if (visible) 1f else 0f,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "focusRing",
    )
    val color = MaterialTheme.colorScheme.primary
    this
        .graphicsLayer {
            shadowElevation = glow.toPx() * alphaState.value
            shape = RoundedCornerShape(corner)
            clip = false
            ambientShadowColor = color
            spotShadowColor = color
        }
        .drawWithContent {
            drawContent()
            val alpha = alphaState.value
            if (alpha > 0f) {
                val w = 3.dp.toPx()
                val r = min(corner.toPx() + w / 2, (min(size.width, size.height) + w) / 2)
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
 *
 * [dpad] 为 false（触摸）时没有放大、没有描边。
 */
internal fun Modifier.bvFocus(
    dpad: Boolean,
    scale: Float = 1.1f,
    corner: Dp = 999.dp,
    glow: Dp = 14.dp,
    addFocusable: Boolean = false,
    onFocused: ((Boolean) -> Unit)? = null,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val show = focused && dpad
    val s by animateFloatAsState(if (show) scale else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "bvFocusScale")
    this
        .zIndex(if (show) 1f else 0f)
        .graphicsLayer { scaleX = s; scaleY = s }
        .focusRing(show, corner, glow)
        .onFocusChanged { focused = it.hasFocus; onFocused?.invoke(it.hasFocus) }
        .then(if (addFocusable) Modifier.focusable() else Modifier)
}
