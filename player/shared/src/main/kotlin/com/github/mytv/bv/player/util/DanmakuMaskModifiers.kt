package com.github.mytv.bv.player.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.toIntSize
import com.caverock.androidsvg.SVG
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuWebMaskFrame

fun Modifier.bitmapMask(bitmap: Bitmap): Modifier = this then BitmapMaskElement(bitmap)

fun Modifier.danmakuWebMask(frame: DanmakuWebMaskFrame): Modifier = this then WebMaskElement(frame)

fun Modifier.danmakuMobMask(frame: DanmakuMobMaskFrame): Modifier = this then MobMaskElement(frame)

fun Modifier.danmakuMask(frame: DanmakuMaskFrame?): Modifier = when (frame) {
    null -> this
    is DanmakuWebMaskFrame -> danmakuWebMask(frame)
    is DanmakuMobMaskFrame -> danmakuMobMask(frame)
}

private data class BitmapMaskElement(
    val bitmap: Bitmap,
) : ModifierNodeElement<BitmapMaskNode>() {
    override fun create() = BitmapMaskNode(bitmap, bitmap)

    override fun update(node: BitmapMaskNode) = node.update(bitmap, bitmap)
}

private data class WebMaskElement(
    val frame: DanmakuWebMaskFrame,
) : ModifierNodeElement<BitmapMaskNode>() {
    override fun create() = BitmapMaskNode(frame, decodeWebMask(frame))

    override fun update(node: BitmapMaskNode) {
        if (!node.matches(frame)) node.update(frame, decodeWebMask(frame))
    }
}

private data class MobMaskElement(
    val frame: DanmakuMobMaskFrame,
) : ModifierNodeElement<BitmapMaskNode>() {
    override fun create() = BitmapMaskNode(frame, decodeMobMask(frame))

    override fun update(node: BitmapMaskNode) {
        if (!node.matches(frame)) node.update(frame, decodeMobMask(frame))
    }
}

private class BitmapMaskNode(
    private var key: Any?,
    bitmap: Bitmap?,
) : Modifier.Node(), DrawModifierNode {

    private var image: ImageBitmap? = bitmap?.asImageBitmap()
    private val paint = Paint()

    fun matches(key: Any?): Boolean = this.key === key

    fun update(key: Any?, bitmap: Bitmap?) {
        if (this.key === key) return
        this.key = key
        this.image = bitmap?.asImageBitmap()
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val current = image
        if (current == null) {
            drawContent()
            return
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            drawImage(
                image = current,
                dstSize = size.toIntSize(),
                blendMode = BlendMode.DstIn,
            )
            canvas.restore()
        }
    }
}

private fun decodeWebMask(frame: DanmakuWebMaskFrame): Bitmap? {
    val svgObj = runCatching { SVG.getFromString(frame.svg) }.getOrNull() ?: return null
    val svgWidth = svgObj.documentWidth.toInt()
    val svgHeight = svgObj.documentHeight.toInt()
    if (svgWidth <= 0 || svgHeight <= 0) return null
    val bitmap = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
    svgObj.renderToCanvas(Canvas(bitmap))
    return bitmap
}

private fun decodeMobMask(frame: DanmakuMobMaskFrame): Bitmap? {
    val width = frame.width
    val height = frame.height
    if (width <= 0 || height <= 0) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // MobMask 是 1bpp 连续 bit 流（MSB first），每位一个像素：
    // 1 = 透明（露出弹幕），0 = 不透明（遮挡弹幕）
    val pixels = IntArray(width * height) { i ->
        val byteIndex = i / 8
        val bitOffset = 7 - (i % 8)
        val bit = (frame.image[byteIndex].toInt() shr bitOffset) and 1
        if (bit == 1) Color.TRANSPARENT else Color.BLACK
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
