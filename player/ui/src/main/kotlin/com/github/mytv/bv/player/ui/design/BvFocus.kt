@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package com.github.mytv.bv.player.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.min

// 第 6.6 节焦点系统的 player:ui 内部版本。
// 与 app:ui 的区别只有一处：不读 LocalDeviceProfile（在 app:ui 里，player:ui 依赖不到），
// 改为由调用方传入 dpad —— 电视，或当前输入模式是键盘（平板接蓝牙键盘）时为 true。

/** 播放器内「是否处于方向键操作」。由 BvPlayerScreen 根据 isTv 与输入模式提供。 */
internal val LocalPlayerDpad = compositionLocalOf { false }

/** 「减少动效」：为 true 时形状变形直接跳到终态（第 6.9 节）。由 BvPlayerScreen 提供。 */
internal val LocalPlayerReduceMotion = compositionLocalOf { false }

/** 播放器内焦点描边颜色，由 BvPlayerScreen 从 MaterialTheme.colorScheme.primary 注入。 */
internal val LocalPlayerFocusColor = staticCompositionLocalOf { Color.Unspecified }

/** 电视，或输入模式为键盘时为 true。 */
@Composable
internal fun rememberDpadMode(isTv: Boolean): Boolean =
    isTv || LocalInputModeManager.current.inputMode == InputMode.Keyboard

/**
 * 外侧 3dp 描边 + 光晕，带淡入淡出动画。
 * 动画值只在 Draw 阶段读取，不触发重组。
 */
internal fun Modifier.focusRing(visible: Boolean, corner: Dp, glow: Dp = 14.dp): Modifier =
    this then FocusRingElement(visible, corner, glow)

private data class FocusRingElement(
    val visible: Boolean,
    val corner: Dp,
    val glow: Dp,
) : ModifierNodeElement<FocusRingNode>() {
    override fun create() = FocusRingNode(visible, corner, glow)

    override fun update(node: FocusRingNode) = node.update(visible, corner, glow)
}

private class FocusRingNode(
    private var visible: Boolean,
    private var corner: Dp,
    private var glow: Dp,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val alpha = Animatable(if (visible) 1f else 0f)

    fun update(visible: Boolean, corner: Dp, glow: Dp) {
        val changed = this.visible != visible
        this.visible = visible
        this.corner = corner
        this.glow = glow
        if (changed) {
            val spec = currentValueOf(MaterialTheme.LocalMotionScheme).fastEffectsSpec<Float>()
            coroutineScope.launch { alpha.animateTo(if (visible) 1f else 0f, spec) }
        }
    }

    override fun ContentDrawScope.draw() {
        val a = alpha.value
        val color = currentValueOf(LocalPlayerFocusColor)
        if (a > 0f && color != Color.Unspecified) {
            val cornerPx = corner.toPx()
            drawFocusGlow(color, a, cornerPx, glow.toPx())
            drawContent()
            drawFocusRing(color, a, cornerPx)
        } else {
            drawContent()
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
): Modifier =
    this
        .then(BvFocusElement(dpad, scale, corner, glow, onFocused))
        .then(if (addFocusable) Modifier.focusable() else Modifier)

private data class BvFocusElement(
    val dpad: Boolean,
    val scale: Float,
    val corner: Dp,
    val glow: Dp,
    val onFocused: ((Boolean) -> Unit)?,
) : ModifierNodeElement<BvFocusNode>() {
    override fun create() = BvFocusNode(dpad, scale, corner, glow, onFocused)

    override fun update(node: BvFocusNode) = node.update(dpad, scale, corner, glow, onFocused)
}

private class BvFocusNode(
    private var dpad: Boolean,
    private var scale: Float,
    private var corner: Dp,
    private var glow: Dp,
    private var onFocused: ((Boolean) -> Unit)?,
) : Modifier.Node(),
    FocusEventModifierNode,
    DrawModifierNode,
    LayoutModifierNode,
    CompositionLocalConsumerModifierNode {

    private var focused = false
    private var show = false
    private val scaleProgress = Animatable(0f)
    private val ringAlpha = Animatable(0f)

    override fun onAttach() {
        refreshShow(animate = false)
    }

    override fun onFocusEvent(focusState: FocusState) {
        val newFocused = focusState.hasFocus
        if (focused == newFocused) return
        focused = newFocused
        onFocused?.invoke(newFocused)
        refreshShow(animate = true)
    }

    fun update(
        dpad: Boolean,
        scale: Float,
        corner: Dp,
        glow: Dp,
        onFocused: ((Boolean) -> Unit)?,
    ) {
        val dpadChanged = this.dpad != dpad
        this.dpad = dpad
        this.scale = scale
        this.corner = corner
        this.glow = glow
        this.onFocused = onFocused
        if (dpadChanged) refreshShow(animate = true)
    }

    private fun refreshShow(animate: Boolean) {
        val newShow = focused && dpad
        if (newShow == show) return
        show = newShow
        // zIndex 在 placement 阶段生效，焦点切换时重新测量一次即可
        invalidateMeasurement()
        val motion = currentValueOf(MaterialTheme.LocalMotionScheme)
        coroutineScope.launch {
            if (animate) {
                launch { scaleProgress.animateTo(if (show) 1f else 0f, motion.fastSpatialSpec<Float>()) }
                launch { ringAlpha.animateTo(if (show) 1f else 0f, motion.fastEffectsSpec<Float>()) }
            } else {
                scaleProgress.snapTo(if (show) 1f else 0f)
                ringAlpha.snapTo(if (show) 1f else 0f)
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val zIndex = if (show) 1f else 0f
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0, zIndex)
        }
    }

    override fun ContentDrawScope.draw() {
        val progress = scaleProgress.value
        val currentScale = 1f + (scale - 1f) * progress
        val alpha = ringAlpha.value
        val color = currentValueOf(LocalPlayerFocusColor)
        val cornerPx = corner.toPx()
        val glowPx = glow.toPx()
        val contentScope = this
        drawScale(currentScale, pivot = center) {
            if (alpha > 0f && color != Color.Unspecified) {
                contentScope.drawFocusGlow(color, alpha, cornerPx, glowPx)
            }
            contentScope.drawContent()
            if (alpha > 0f && color != Color.Unspecified) {
                contentScope.drawFocusRing(color, alpha, cornerPx)
            }
        }
    }
}

private fun ContentDrawScope.drawFocusRing(color: Color, alpha: Float, cornerPx: Float) {
    val width = 3.dp.toPx()
    val radius = min(
        cornerPx + width / 2,
        (min(size.width, size.height) + width) / 2,
    )
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(-width / 2, -width / 2),
        size = Size(size.width + width, size.height + width),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width),
    )
}

/**
 * 用多层外扩描边模拟原来的 elevation 阴影光晕：颜色跟随主题 primary，
 * 透明度随焦点动画淡入淡出，且不依赖 graphicsLayer。
 */
private fun ContentDrawScope.drawFocusGlow(
    color: Color,
    alpha: Float,
    cornerPx: Float,
    glowPx: Float,
) {
    if (glowPx <= 0f) return
    val layers = 6
    val step = glowPx / layers
    for (i in layers downTo 1) {
        val spread = step * i
        drawRoundRect(
            color = color.copy(alpha = alpha * 0.05f),
            topLeft = Offset(-spread, -spread),
            size = Size(size.width + spread * 2, size.height + spread * 2),
            cornerRadius = CornerRadius(cornerPx + spread),
            style = Stroke(step * 1.6f),
        )
    }
}
