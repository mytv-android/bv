package dev.aaa1115910.bv.player.ui.touch

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * 全屏触摸端双指缩放状态。
 *
 * 画面通过改变 SurfaceView 的布局尺寸缩放（而不是 graphicsLayer），
 * 所以只适用于全屏：内嵌播放器放大后会盖到页面内容上。
 */
@Stable
internal class VideoZoomState {
    var scale by mutableFloatStateOf(1f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    /** 视频基础布局尺寸（px） */
    var contentSize: IntSize = IntSize.Zero

    /** 手势层尺寸（px），决定平移边界 */
    var viewportSize: IntSize = IntSize.Zero

    /** 手势层中心（px），缩放焦点以此为基准 */
    var gestureCenter: Offset = Offset.Zero

    val isZoomed: Boolean
        get() = scale > 1.001f

    fun onZoom(centroid: Offset, pan: Offset, zoom: Float) {
        if (contentSize == IntSize.Zero || viewportSize == IntSize.Zero) return
        val newScale = (scale * zoom).coerceIn(MinScale, MaxScale)
        val factor = newScale / scale
        val delta = centroid - gestureCenter
        offset = clamp(offset * factor + delta * (1f - factor) + pan, newScale)
        scale = newScale
    }

    fun onPan(delta: Offset) {
        if (!isZoomed) return
        offset = clamp(offset + delta, scale)
    }

    fun onGestureEnd() {
        if (scale < SettleThreshold) {
            reset()
        } else {
            offset = clamp(offset, scale)
        }
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    private fun clamp(value: Offset, scale: Float): Offset {
        // 只有放大后的画面超出屏幕的那个轴才允许平移，避免露出黑边
        val maxX = ((contentSize.width * scale - viewportSize.width) / 2f).coerceAtLeast(0f)
        val maxY = ((contentSize.height * scale - viewportSize.height) / 2f).coerceAtLeast(0f)
        return Offset(
            value.x.coerceIn(-maxX, maxX),
            value.y.coerceIn(-maxY, maxY),
        )
    }

    private companion object {
        const val MinScale = 1f
        const val MaxScale = 3f
        const val SettleThreshold = 1.05f
    }
}
