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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.absoluteValue

/**
 * 播放器手势，照搬旧 player/mobile BvPlayerController 的 detectPlayerGestures：
 * 单击、双击、长按（松手结束）、横滑快进、左半竖滑亮度、右半竖滑音量。
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
): Modifier = composed {
    val currentSpeedState = rememberUpdatedState(currentSpeed)
    val onTapState = rememberUpdatedState(onTap)
    val onLongPressState = rememberUpdatedState(onLongPress)
    val onLongPressEndState = rememberUpdatedState(onLongPressEnd)
    val onDoubleTapState = rememberUpdatedState(onDoubleTap)
    val onVolumeDragState = rememberUpdatedState(onVolumeDrag)
    val onBrightnessDragState = rememberUpdatedState(onBrightnessDrag)
    val onSeekDragState = rememberUpdatedState(onSeekDrag)
    val onDragEndState = rememberUpdatedState(onDragEnd)

    var oldPlaySpeed by remember { mutableFloatStateOf(1f) }
    var componentWidth by remember { mutableIntStateOf(0) }
    var componentHeight by remember { mutableIntStateOf(0) }
    val horizontalSafetyArea = 0.1f
    val verticalSafetyArea = 0.2f
    var determinedDirection by remember { mutableStateOf(false) }
    var isHorizontal by remember { mutableStateOf(false) }
    var horizontalPointMove by remember { mutableFloatStateOf(0f) }
    var verticalPointMove by remember { mutableFloatStateOf(0f) }
    var inSafetyArea by remember { mutableStateOf(false) }
    var longPressing by remember { mutableStateOf(false) }
    var isMovingVolume by remember { mutableStateOf(false) }
    var isMovingBrightness by remember { mutableStateOf(false) }
    var isPanning by remember { mutableStateOf(false) }

    pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                if (longPressing) return@detectTapGestures
                onTapState.value()
            },
            onLongPress = {
                onLongPressState.value()
                oldPlaySpeed = currentSpeedState.value
                longPressing = true
            },
            onDoubleTap = {
                if (longPressing) return@detectTapGestures
                onDoubleTapState.value()
            },
            onPress = {
                tryAwaitRelease()
                if (longPressing) onLongPressEndState.value(oldPlaySpeed)
                longPressing = false
            }
        )
    }
        .onSizeChanged { size ->
            componentWidth = size.width
            componentHeight = size.height
        }
        .pointerInput(enableSafetyArea) {
            if (longPressing) return@pointerInput

            detectDragGestures(
                onDragStart = {
                    isPanning = false
                    if (zoomState?.isZoomed == true) {
                        isPanning = true
                        return@detectDragGestures
                    }

                    val inHorizontalSafetyArea =
                        it.x > componentWidth * horizontalSafetyArea * 0.5f && it.x < componentWidth * (1 - horizontalSafetyArea * 0.5f)
                    val inVerticalSafetyArea =
                        it.y > componentHeight * verticalSafetyArea * 0.5f && it.y < componentHeight * (1 - verticalSafetyArea * 0.5f)
                    inSafetyArea =
                        inHorizontalSafetyArea && inVerticalSafetyArea || !enableSafetyArea
                    if (!inSafetyArea) return@detectDragGestures

                    if (it.x < componentWidth * 0.5f) {
                        isMovingBrightness = true
                    } else if (it.x >= componentWidth * 0.5f) {
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
                        onDragEndState.value(0f, 0f, horizontalPointMove)
                    } else {
                        if (isMovingVolume) {
                            onDragEndState.value(verticalPointMove, 0f, 0f)
                        } else if (isMovingBrightness) {
                            onDragEndState.value(0f, verticalPointMove, 0f)
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
                }
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
                        onSeekDragState.value(horizontalPointMove)
                    } else {
                        if (isMovingVolume) {
                            onVolumeDragState.value(verticalPointMove)
                        } else if (isMovingBrightness) {
                            onBrightnessDragState.value(verticalPointMove)
                        }
                    }
                }
            }
        }
        .pointerInput(zoomState) {
            val state = zoomState ?: return@pointerInput
            detectPinchGestures(
                onStart = {
                    state.viewportSize = IntSize(componentWidth, componentHeight)
                    state.gestureCenter = Offset(componentWidth / 2f, componentHeight / 2f)
                },
                onGesture = { centroid, pan, zoom -> state.onZoom(centroid, pan, zoom) },
                onEnd = { state.onGestureEnd() },
            )
        }
}

/**
 * 双指捏合 + 平移。只有第二根手指落下后才消费事件，
 * 从而取消单击/长按/单指拖动，不影响其它手势。
 */
private suspend fun PointerInputScope.detectPinchGestures(
    onStart: () -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onEnd: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var zooming = false
        var pastSlop = false
        var pendingPan = Offset.Zero
        var pendingZoom = 1f
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                // 第二指落下立即消费，取消 tap / 长按 / seek / 亮度 / 音量
                event.changes.forEach { it.consume() }
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
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
                        onStart()
                    }
                    onGesture(
                        event.calculateCentroid(useCurrent = true),
                        if (isFirst) pendingPan else panChange,
                        if (isFirst) pendingZoom else zoomChange,
                    )
                    pendingPan = Offset.Zero
                    pendingZoom = 1f
                }
            }
        } while (event.changes.any { it.pressed })
        if (zooming) onEnd()
    }
}

private const val ZoomSlop = 0.02f
