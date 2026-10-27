package dev.aaa1115910.m3qrcode

import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.withTranslation
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.github.mytv.bv.R
import kotlinx.coroutines.isActive
import kotlin.math.min

@Composable
fun MaterialShapeQr(
    modifier: Modifier = Modifier,
    content: String,
    ecLevel: MaterialShapeQrErrorCorrectionLevel = MaterialShapeQrErrorCorrectionLevel.M,
    colorScheme: ColorScheme? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val fallbackColorScheme = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            androidx.compose.material3.dynamicLightColorScheme(context)
        } else {
            androidx.compose.material3.lightColorScheme()
        }
    }
    val state = rememberMaterialShapeQrState(
        colorScheme = colorScheme ?: fallbackColorScheme
    )
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_qrcode_background))
    val progress = animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    val lottieDynamicProperties = rememberMaterialShapeQrLottieDynamicProperties(state.colorMap)

    // 生成与帧循环属于同一代内容，取消旧编码不会让新动画提前退出。
    // 逐帧值只由绘制层读取；保留 Android 12+ 定位图案的持续旋转。
    LaunchedEffect(state, content, ecLevel) {
        state.updateContent(content, ecLevel.level)
        if (state.qrcodeLineCount == 0) return@LaunchedEffect
        while (isActive) {
            withFrameMillis {
                val elapsed = System.currentTimeMillis() - state.qrStartTime
                state.frameElapsed = elapsed
                state.dataFrameElapsed = elapsed.coerceAtMost(3000L)
                state.drawAnimationBackground(elapsed)
                if (elapsed >= 3000L) {
                    state.hasFinalDataImage = true
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && state.hasFinalDataImage) {
                break
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            modifier = Modifier.graphicsLayer {
                scaleX = state.lottieScale
                scaleY = state.lottieScale
            },
            composition = composition,
            progress = { progress.value },
            dynamicProperties = lottieDynamicProperties
        )
        if (view.isInEditMode) {
            LottieAnimation(
                composition = composition,
                progress = { progress.value },
                dynamicProperties = lottieDynamicProperties
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
        ) {
            NonFinderPatternsCanvas(
                qrcodeLineCount = state.qrcodeLineCount,
                qrcodeSize = state.qrcodeSize,
                frameElapsed = { state.dataFrameElapsed },
                dataModuleShapeList = state.dataModuleShapeList,
                backgroundModuleShapeList = state.backgroundModuleShapeList,
            )
            FinderPatternsCanvas(
                qrcodeLineCount = state.qrcodeLineCount,
                qrcodeSize = state.qrcodeSize,
                frameElapsed = { state.frameElapsed },
                finderPatternShapeList = state.finderPatternShapeList
            )
        }
    }
}

@Composable
fun NonFinderPatternsCanvas(
    modifier: Modifier = Modifier,
    qrcodeLineCount: Int,
    qrcodeSize: Int,
    frameElapsed: () -> Long,
    dataModuleShapeList: List<MaterialShapeRenderer>,
    backgroundModuleShapeList: List<MaterialShapeRenderer>,
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val native = drawContext.canvas.nativeCanvas

        if (qrcodeLineCount == 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        val scale = min(canvasWidth / qrcodeSize.toFloat(), canvasHeight / qrcodeSize.toFloat())
        val scaledWidth = qrcodeSize * scale
        val scaledHeight = qrcodeSize * scale

        val tx = (canvasWidth - scaledWidth) / 2f
        val ty = (canvasHeight - scaledHeight) / 2f

        // 只读帧状态并夹到动画终点，不在 Draw 阶段写任何 Compose 状态。
        val drawElapsed = frameElapsed().coerceAtMost(3000L)

        native.withTranslation(tx, ty) {
            scale(scale, scale)
            drawShapeListOnCanvas(dataModuleShapeList, this, drawElapsed)
            drawShapeListOnCanvas(backgroundModuleShapeList, this, drawElapsed)
        }
    }
}

@Composable
fun FinderPatternsCanvas(
    modifier: Modifier = Modifier,
    qrcodeLineCount: Int,
    qrcodeSize: Int,
    frameElapsed: () -> Long,
    finderPatternShapeList: List<MaterialShapeRenderer>
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val native = drawContext.canvas.nativeCanvas

        if (qrcodeLineCount == 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        val scale = min(canvasWidth / qrcodeSize.toFloat(), canvasHeight / qrcodeSize.toFloat())
        val scaledWidth = qrcodeSize * scale
        val scaledHeight = qrcodeSize * scale

        val tx = (canvasWidth - scaledWidth) / 2f
        val ty = (canvasHeight - scaledHeight) / 2f

        val elapsed = frameElapsed()

        native.withTranslation(tx, ty) {
            scale(scale, scale)
            drawShapeListOnCanvas(finderPatternShapeList, this, elapsed)
        }
    }
}

@Composable
private fun rememberMaterialShapeQrLottieDynamicProperties(
    colorMap: Map<String, Int>
): com.airbnb.lottie.compose.LottieDynamicProperties {
    val properties = colorMap.map { (key, color) ->
        val filter = remember(color) { PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN) }
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            keyPath = arrayOf("**", key, "**"),
        ) {
            filter
        }
    }.toTypedArray()
    return rememberLottieDynamicProperties(*properties)
}

private fun drawShapeListOnCanvas(
    list: List<MaterialShapeRenderer>,
    canvas: Canvas,
    elapsedMs: Long
) {
    list.fastForEach { it.draw(canvas, elapsedMs) }
}
