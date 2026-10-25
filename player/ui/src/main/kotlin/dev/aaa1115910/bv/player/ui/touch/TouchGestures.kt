@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.touch

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.IntSize
import kotlin.math.absoluteValue

/**
 * 播放器手势，照搬旧 player/mobile BvPlayerController 的 detectPlayerGestures：
 * 单击、双击、长按（松手结束）、横滑快进、左半竖滑亮度、右半竖滑音量。
 *
 * 手势状态全部放在 Modifier.Node 的普通字段里，不再用 composed + remember 状态。
 */
internal fun Modifier.detectPlayerGestures(
    enableSafetyArea: Boolean,
    currentSpeed: Float,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressEnd: (speed: Float) -> Unit,
    onDoubleTap: () -> Unit,
    onVolumeDrag: (move: Float) -> Unit,
    onBrightnessDrag: (move: Float) -> Unit,
    onSeekDrag: (move: Float) -> Unit,
    onDragEnd: (volumeMove: Float, brightnessMove: Float, seekMove: Float) -> Unit,
    zoomState: VideoZoomState? = null,
): Modifier = this then PlayerGesturesElement(
    enableSafetyArea = enableSafetyArea,
    currentSpeed = currentSpeed,
    onTap = onTap,
    onLongPress = onLongPress,
    onLongPressEnd = onLongPressEnd,
    onDoubleTap = onDoubleTap,
    onVolumeDrag = onVolumeDrag,
    onBrightnessDrag = onBrightnessDrag,
    onSeekDrag = onSeekDrag,
    onDragEnd = onDragEnd,
    zoomState = zoomState,
)

private data class PlayerGesturesElement(
    val enableSafetyArea: Boolean,
    val currentSpeed: Float,
    val onTap: () -> Unit,
    val onLongPress: () -> Unit,
    val onLongPressEnd: (Float) -> Unit,
    val onDoubleTap: () -> Unit,
    val onVolumeDrag: (Float) -> Unit,
    val onBrightnessDrag: (Float) -> Unit,
    val onSeekDrag: (Float) -> Unit,
    val onDragEnd: (Float, Float, Float) -> Unit,
    val zoomState: VideoZoomState?,
) : ModifierNodeElement<PlayerGesturesNode>() {
    override fun create() = PlayerGesturesNode(
        enableSafetyArea = enableSafetyArea,
        currentSpeed = currentSpeed,
        onTap = onTap,
        onLongPress = onLongPress,
        onLongPressEnd = onLongPressEnd,
        onDoubleTap = onDoubleTap,
        onVolumeDrag = onVolumeDrag,
        onBrightnessDrag = onBrightnessDrag,
        onSeekDrag = onSeekDrag,
        onDragEnd = onDragEnd,
        zoomState = zoomState,
    )

    override fun update(node: PlayerGesturesNode) = node.update(
        enableSafetyArea = enableSafetyArea,
        currentSpeed = currentSpeed,
        onTap = onTap,
        onLongPress = onLongPress,
        onLongPressEnd = onLongPressEnd,
        onDoubleTap = onDoubleTap,
        onVolumeDrag = onVolumeDrag,
        onBrightnessDrag = onBrightnessDrag,
        onSeekDrag = onSeekDrag,
        onDragEnd = onDragEnd,
        zoomState = zoomState,
    )
}

private class PlayerGesturesNode(
    private var enableSafetyArea: Boolean,
    private var currentSpeed: Float,
    private var onTap: () -> Unit,
    private var onLongPress: () -> Unit,
    private var onLongPressEnd: (Float) -> Unit,
    private var onDoubleTap: () -> Unit,
    private var onVolumeDrag: (Float) -> Unit,
    private var onBrightnessDrag: (Float) -> Unit,
    private var onSeekDrag: (Float) -> Unit,
    private var onDragEnd: (Float, Float, Float) -> Unit,
    private var zoomState: VideoZoomState?,
) : DelegatingNode(), LayoutAwareModifierNode {

    private var componentWidth = 0
    private var componentHeight = 0
    private var oldPlaySpeed = 1f
    private var determinedDirection = false
    private var isHorizontal = false
    private var horizontalPointMove = 0f
    private var verticalPointMove = 0f
    private var inSafetyArea = false
    private var longPressing = false
    private var isMovingVolume = false
    private var isMovingBrightness = false
    private var isPanning = false

    private val tapNode = delegate(
        SuspendingPointerInputModifierNode {
            detectTapGestures(
                onTap = {
                    if (!longPressing) onTap()
                },
                onLongPress = {
                    onLongPress()
                    oldPlaySpeed = currentSpeed
                    longPressing = true
                },
                onDoubleTap = {
                    if (!longPressing) onDoubleTap()
                },
                onPress = {
                    tryAwaitRelease()
                    if (longPressing) onLongPressEnd(oldPlaySpeed)
                    longPressing = false
                },
            )
        }
    )

    private val dragNode = delegate(
        SuspendingPointerInputModifierNode {
            detectDragGestures(
                onDragStart = {
                    isPanning = false
                    if (zoomState?.isZoomed == true) {
                        isPanning = true
                        return@detectDragGestures
                    }
                    if (longPressing) return@detectDragGestures

                    val horizontalSafetyArea = 0.1f
                    val verticalSafetyArea = 0.2f
                    val inHorizontalSafetyArea =
                        it.x > componentWidth * horizontalSafetyArea * 0.5f &&
                            it.x < componentWidth * (1 - horizontalSafetyArea * 0.5f)
                    val inVerticalSafetyArea =
                        it.y > componentHeight * verticalSafetyArea * 0.5f &&
                            it.y < componentHeight * (1 - verticalSafetyArea * 0.5f)
                    inSafetyArea =
                        inHorizontalSafetyArea && inVerticalSafetyArea || !enableSafetyArea
                    if (!inSafetyArea) return@detectDragGestures

                    if (it.x < componentWidth * 0.5f) {
                        isMovingBrightness = true
                    } else {
                        isMovingVolume = true
                    }
                },
                onDragEnd = {
                    if (isPanning) {
                        isPanning = false
                        return@detectDragGestures
                    }
                    if (!inSafetyArea) return@detectDragGestures

                    if (isHorizontal) {
                        onDragEnd(0f, 0f, horizontalPointMove)
                    } else {
                        if (isMovingVolume) {
                            onDragEnd(verticalPointMove, 0f, 0f)
                        } else if (isMovingBrightness) {
                            onDragEnd(0f, verticalPointMove, 0f)
                        }
                    }

                    horizontalPointMove = 0f
                    verticalPointMove = 0f
                    determinedDirection = false
                    isMovingVolume = false
                    isMovingBrightness = false
                },
                onDragCancel = {
                    isPanning = false
                    horizontalPointMove = 0f
                    verticalPointMove = 0f
                    determinedDirection = false
                    isMovingVolume = false
                    isMovingBrightness = false
                    inSafetyArea = false
                },
            ) { change, dragAmount ->
                if (isPanning) {
                    zoomState?.onPan(dragAmount)
                    change.consume()
                    return@detectDragGestures
                }
                if (!inSafetyArea) return@detectDragGestures
                horizontalPointMove += dragAmount.x
                verticalPointMove += dragAmount.y
                if (!determinedDirection) {
                    if (horizontalPointMove.absoluteValue > 20f) {
                        determinedDirection = true
                        isHorizontal = true
                    } else if (verticalPointMove.absoluteValue > 20f) {
                        determinedDirection = true
                        isHorizontal = false
                    }
                }
                if (determinedDirection) {
                    if (isHorizontal) {
                        onSeekDrag(horizontalPointMove)
                    } else {
                        if (isMovingVolume) {
                            onVolumeDrag(verticalPointMove)
                        } else if (isMovingBrightness) {
                            onBrightnessDrag(verticalPointMove)
                        }
                    }
                }
            }
        }
    )

    private val pinchNode = delegate(
        SuspendingPointerInputModifierNode {
            detectPinchGestures(
                zoomState = { zoomState },
                onStart = { state ->
                    state.viewportSize = IntSize(componentWidth, componentHeight)
                    state.gestureCenter = Offset(componentWidth / 2f, componentHeight / 2f)
                },
                onGesture = { state, centroid, pan, zoom -> state.onZoom(centroid, pan, zoom) },
                onEnd = { state -> state.onGestureEnd() },
            )
        }
    )

    override fun onRemeasured(size: IntSize) {
        componentWidth = size.width
        componentHeight = size.height
    }

    fun update(
        enableSafetyArea: Boolean,
        currentSpeed: Float,
        onTap: () -> Unit,
        onLongPress: () -> Unit,
        onLongPressEnd: (Float) -> Unit,
        onDoubleTap: () -> Unit,
        onVolumeDrag: (Float) -> Unit,
        onBrightnessDrag: (Float) -> Unit,
        onSeekDrag: (Float) -> Unit,
        onDragEnd: (Float, Float, Float) -> Unit,
        zoomState: VideoZoomState?,
    ) {
        this.enableSafetyArea = enableSafetyArea
        this.currentSpeed = currentSpeed
        this.onTap = onTap
        this.onLongPress = onLongPress
        this.onLongPressEnd = onLongPressEnd
        this.onDoubleTap = onDoubleTap
        this.onVolumeDrag = onVolumeDrag
        this.onBrightnessDrag = onBrightnessDrag
        this.onSeekDrag = onSeekDrag
        this.onDragEnd = onDragEnd
        this.zoomState = zoomState
    }
}

/**
 * 双指捏合 + 平移。只有第二根手指落下后才消费事件，
 * 从而取消单击/长按/单指拖动，不影响其它手势。
 */
private suspend fun PointerInputScope.detectPinchGestures(
    zoomState: () -> VideoZoomState?,
    onStart: (VideoZoomState) -> Unit,
    onGesture: (VideoZoomState, centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onEnd: (VideoZoomState) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val state = zoomState() ?: return@awaitEachGesture
        var zooming = false
        var pastSlop = false
        var pendingPan = Offset.Zero
        var pendingZoom = 1f
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                // 先计算再消费：calculate* 只统计未消费的指针，消费后再算会恒等于无手势
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = true)
                // 第二指落下立即消费，取消 tap / 长按 / seek / 亮度 / 音量
                event.changes.forEach { it.consume() }
                pendingPan += panChange
                pendingZoom *= zoomChange
                if (!pastSlop) {
                    pastSlop = (1f - pendingZoom).absoluteValue > ZoomSlop ||
                        pendingPan.getDistance() > viewConfiguration.touchSlop
                }
                if (pastSlop) {
                    val isFirst = !zooming
                    if (isFirst) {
                        zooming = true
                        onStart(state)
                    }
                    onGesture(
                        state,
                        centroid,
                        if (isFirst) pendingPan else panChange,
                        if (isFirst) pendingZoom else zoomChange,
                    )
                    pendingPan = Offset.Zero
                    pendingZoom = 1f
                }
            }
        } while (event.changes.any { it.pressed })
        if (zooming) onEnd(state)
    }
}

private const val ZoomSlop = 0.02f
