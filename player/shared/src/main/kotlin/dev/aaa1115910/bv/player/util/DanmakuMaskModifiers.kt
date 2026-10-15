package dev.aaa1115910.bv.player.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.toIntSize
import com.caverock.androidsvg.SVG
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuWebMaskFrame

fun Modifier.bitmapMask(
    bitmap: Bitmap
): Modifier = composed {
    drawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            drawContent()
            drawImage(
                image = bitmap.asImageBitmap(),
                dstSize = size.toIntSize(),
                blendMode = BlendMode.DstIn
            )
            canvas.restore()
        }
    }
}

fun Modifier.danmakuWebMask(
    frame: DanmakuWebMaskFrame
): Modifier = composed {
    val svgObj = runCatching {
        SVG.getFromString(frame.svg)
    }.getOrNull() ?: return@composed this

    val svgWidth = svgObj.documentWidth.toInt()
    val svgHeight = svgObj.documentHeight.toInt()

    val bitmap = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    svgObj.renderToCanvas(canvas)

    bitmapMask(bitmap)
}

fun Modifier.danmakuMobMask(
    frame: DanmakuMobMaskFrame
): Modifier = composed {
    // remember(frame) 保证像素解码和 Bitmap 创建只在帧变化时执行一次，
    // 避免每次 recompose 都重建 Bitmap 并逐像素 setPixel
    val binaryBitmap = remember(frame) {
        val width = frame.width
        val height = frame.height
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
        bitmap
    }

    bitmapMask(binaryBitmap)
}

fun Modifier.danmakuMask(
    frame: DanmakuMaskFrame?
): Modifier = composed {
    if (frame == null) return@composed this

    when (frame) {
        is DanmakuWebMaskFrame -> danmakuWebMask(frame)
        is DanmakuMobMaskFrame -> danmakuMobMask(frame)
    }
}