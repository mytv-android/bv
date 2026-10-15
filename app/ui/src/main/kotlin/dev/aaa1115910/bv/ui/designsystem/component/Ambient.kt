package dev.aaa1115910.bv.ui.designsystem.component

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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.BlurTransformation

private val SaturationFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.4f) })

/**
 * 原型 `.m-ambient`（氛围光）：封面铺满父容器并四周外扩 10%，放大 1.1、透明度 0.55、饱和度 1.4、模糊 48dp。
 * API < 31 用 BlurTransformation（64px，radius 25）；低内存设备只画 surface 纯色。
 */
@Composable
fun Ambient(cover: String?, modifier: Modifier = Modifier) {
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
            val request = remember(url, modern) {
                ImageRequest.Builder(context).data(url).apply {
                    if (!modern) {
                        size(64)
                        transformations(BlurTransformation(context, 25f))
                    }
                }.build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = SaturationFilter,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val w = constraints.maxWidth
                        val h = constraints.maxHeight
                        val placeable = measurable.measure(
                            Constraints.fixed((w * 1.2f).toInt(), (h * 1.2f).toInt())
                        )
                        layout(w, h) { placeable.place(-(w * 0.1f).toInt(), -(h * 0.1f).toInt()) }
                    }
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                        alpha = 0.55f
                    }
                    .then(if (modern) Modifier.blur(48.dp) else Modifier),
            )
        }
    }
}
