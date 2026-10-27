package com.github.mytv.bv.ui.designsystem.component

import android.app.ActivityManager
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.BlurTransformation

private val SaturationFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.4f) })

/** 模糊层按屏幕的 30% 绘制，再放大 4.4 倍：48dp×1.1 的最终模糊半径不变，离屏像素少 ~19 倍。 */
private const val AmbientLayerFraction = 0.3f
private const val AmbientLayerScale = 4.4f
private const val AmbientLayerTranslate = 1.1667f
private val AmbientBlurRadius = 12.dp

/**
 * 原型 `.m-ambient`（氛围光）：封面铺满父容器并四周外扩 10%，放大 1.1、透明度 0.55、饱和度 1.4、模糊 48dp。
 * API < 31 用 BlurTransformation（64px，radius 25）；低内存设备只画 surface 纯色。
 *
 * 模糊在 30% 尺寸的图层上完成再放大，视觉结果与原全尺寸 RenderEffect 一致，但 GPU 开销大幅降低。
 */
@Composable
fun Ambient(
    cover: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.55f,
) {
    val context = LocalContext.current
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false
    }
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (lowRam || cover.isNullOrEmpty()) return@Box
        Crossfade(
            targetState = cover,
            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            label = "ambient",
        ) { url ->
            val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val layerAlpha = alpha
            val request = remember(url, modern) {
                ImageRequest.Builder(context).data(url).apply {
                    if (!modern) {
                        size(64)
                        transformations(BlurTransformation(context, 25f))
                    }
                }.build()
            }
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize(AmbientLayerFraction)
                        .graphicsLayer {
                            scaleX = AmbientLayerScale
                            scaleY = AmbientLayerScale
                            translationX = size.width * AmbientLayerTranslate
                            translationY = size.height * AmbientLayerTranslate
                            this.alpha = layerAlpha
                        }
                        .then(if (modern) Modifier.blur(AmbientBlurRadius) else Modifier)
                ) {
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = SaturationFilter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
