package dev.aaa1115910.m3qrcode

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.withRotation
import androidx.core.graphics.withSave
import kotlin.math.cos

class MaterialShapeRenderer {
    var animationStyle: EntryAnimationStyle
    var destRect: RectF
    var duration: Long = 0
    var initialRotation: Int = 0
    var isMotionPaused: Boolean = false
    var paint: Paint
    var skipStartProgress: Float = 0f
    var srcImgSvg: Drawable
    var startDelay: Long = 0

    private val scratchRect = Rect()
    private val scratchRectF = RectF()
    private val renderDrawable: Drawable by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            runCatching { srcImgSvg.constantState!!.newDrawable().mutate() }
                .getOrDefault(srcImgSvg)
        } else {
            srcImgSvg
        }
    }

    companion object {
        private var springScaleCache: LinkedHashMap<Int, Float> = linkedMapOf()
        private var dampedString: DampedString = DampedString(60, 0.63f)
        fun calculateSpringScale(j: Long): Float {
            val i = (j / 16).toInt()
            if (springScaleCache.containsKey(i)) {
                return springScaleCache[i]!!
            }
            val calculatePosition = dampedString.calculatePosition(i)
            springScaleCache[i] = calculatePosition
            return calculatePosition
        }
    }

    constructor(srcImgSvg: Drawable, destRect: RectF, paint: Paint) {
        this.srcImgSvg = srcImgSvg
        this.destRect = destRect
        this.paint = paint
        this.animationStyle = EntryAnimationStyle.None
    }

    fun draw(canvas: Canvas, elapsedMs: Long) {
        val delay = startDelay
        if (elapsedMs < delay) return
        val adjustedMs = elapsedMs - delay
        when (animationStyle) {
            EntryAnimationStyle.None -> drawForNone(canvas, adjustedMs)
            EntryAnimationStyle.ZoomIn -> drawForZoomIn(canvas, adjustedMs)
            EntryAnimationStyle.SpringZoomIn -> drawForSpringZoomIn(canvas, adjustedMs)
            EntryAnimationStyle.RotateEmphasizedZoomIn -> drawForRotateEmphasizedZoomIn(
                canvas,
                adjustedMs
            )

            EntryAnimationStyle.EmphasizedZoomIn -> drawForEmphasizedZoomIn(canvas, adjustedMs)
        }
    }

    private fun draw(canvas: Canvas, rectF: RectF, paint: Paint) {
        canvas.withRotation(initialRotation * 90.0f, rectF.centerX(), rectF.centerY()) {
            rectF.round(scratchRect)
            renderDrawable.bounds = scratchRect
            renderDrawable.colorFilter = paint.colorFilter
            renderDrawable.draw(this)
        }
    }

    private fun drawScaled(canvas: Canvas, scale: Float, paint: Paint) {
        val halfWidth = destRect.width() / 2.0f * scale
        val halfHeight = destRect.height() / 2.0f * scale
        scratchRectF.set(
            destRect.centerX() - halfWidth,
            destRect.centerY() - halfHeight,
            destRect.centerX() + halfWidth,
            destRect.centerY() + halfHeight,
        )
        draw(canvas, scratchRectF, paint)
    }

    private fun drawForNone(canvas: Canvas, j: Long) {
        draw(canvas, destRect, paint)
    }

    private fun drawForZoomIn(canvas: Canvas, j: Long) {
        val j2 = duration
        val f = if (j2 > 0) j2.toFloat() else 1000f
        val f2 = j.toFloat()
        if (f2 / f < skipStartProgress) {
            return
        }
        if (f2 <= f) {
            val cos = (cos(((f2 - 1000.0f) / 1000.0f) * 3.1415927f) + 1.0f) / 2.0f
            drawScaled(canvas, cos, paint)
        } else {
            drawForNone(canvas, j)
        }
    }

    private fun drawForEmphasizedZoomIn(canvas: Canvas, j: Long) {
        val j2 = duration
        val f = if (j2 > 0) j2.toFloat() else 1000f
        val f2 = j.toFloat()
        if (f2 <= f) {
            val interpolation = EmphasizedInterpolator.getInterpolation(f2 / f)
            drawScaled(canvas, interpolation, paint)
        } else {
            drawForNone(canvas, j)
        }
    }

    private fun drawForSpringZoomIn(canvas: Canvas, j: Long) {
        if (j <= 1500) {
            val springScale = calculateSpringScale(j)
            drawScaled(canvas, springScale, paint)
        } else {
            drawForNone(canvas, j)
        }
    }

    private fun drawForRotateEmphasizedZoomIn(canvas: Canvas, j: Long) {
        canvas.withSave {
            val j2 = duration
            val f = if (j2 > 0) j2.toFloat() else 1000f
            val f2 = j.toFloat()
            var f3 = ((f2 - f) * 360.0f) / 4410.0f
            if (f2 <= f) {
                val interpolation = EmphasizedInterpolator.getInterpolation(f2 / f)
                rotate(
                    (interpolation * 180.0f) + f3,
                    destRect.centerX(),
                    destRect.centerY()
                )
                drawScaled(this, interpolation, paint)
            } else {
                if (isMotionPaused) {
                    f3 = 0.0f
                }
                rotate(f3, destRect.centerX(), destRect.centerY())
                draw(this, destRect, paint)
            }
        }
    }
}
