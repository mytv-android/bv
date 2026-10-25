package dev.aaa1115910.bv.ui.imageviewer.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import dev.aaa1115910.bv.ui.imageviewer.previewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC

class RawGesture(
    val onTap: (Offset) -> Unit = {},
    val onDoubleTap: (Offset) -> Unit = {},
    val onLongPress: (Offset) -> Unit = {},
    val gestureStart: () -> Unit = {},
    val gestureEnd: (transformOnly: Boolean) -> Unit = {},
    val onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, event: PointerEvent) -> Boolean = { _, _, _, _, _ -> true },
)

data class SizeChangeContent(
    val defaultSize: IntSize,
    val containerSize: IntSize,
    val maxScale: Float,
)

@Composable
fun ImageComposeOrigin(
    modifier: Modifier = Modifier,
    model: Any,
    scale: () -> Float = { DEFAULT_SCALE },
    offsetX: () -> Float = { DEFAULT_OFFSET_X },
    offsetY: () -> Float = { DEFAULT_OFFSET_Y },
    rotation: () -> Float = { DEFAULT_ROTATION },
    gesture: RawGesture = RawGesture(),
    onMounted: () -> Unit = {},
    onSizeChange: suspend (SizeChangeContent) -> Unit = {},
    crossfadeAnimationSpec: AnimationSpec<Float> = DEFAULT_CROSS_FADE_ANIMATE_SPEC,
    boundClip: Boolean = true,
) {
    // 容器大小
    var bSize by remember { mutableStateOf(IntSize(0, 0)) }
    val density = LocalDensity.current
    val imageSpecified = model !is Painter || model.intrinsicSize.isSpecified
    val oSize = when (model) {
        is Painter -> if (imageSpecified) IntSize(
            model.intrinsicSize.width.toInt(), model.intrinsicSize.height.toInt(),
        ) else IntSize.Zero
        is ImageVector -> with(density) {
            IntSize(model.defaultWidth.roundToPx(), model.defaultHeight.roundToPx())
        }
        is ImageBitmap -> IntSize(model.width, model.height)
        is ComposeModel -> model.intrinsicSize.takeUnless { it == IntSize.Zero } ?: bSize
        else -> error("This model type is not supported!")
    }
    val bRatio = bSize.width.toFloat() / bSize.height
    val oRatio = oSize.width.toFloat() / oSize.height
    val widthFixed = oRatio > bRatio
    val superSize = oSize.height > bSize.height && oSize.width > bSize.width
    val uSize = when {
        oSize.width <= 0 || oSize.height <= 0 -> IntSize.Zero
        widthFixed -> IntSize(bSize.width, (bSize.width / oRatio).toInt())
        else -> IntSize((bSize.height * oRatio).toInt(), bSize.height)
    }
    val latestOnSizeChange by rememberUpdatedState(onSizeChange)
    LaunchedEffect(key1 = oSize, key2 = bSize, key3 = uSize) {
        if (oSize.width <= 0 || oSize.height <= 0 || bSize.width <= 0 || bSize.height <= 0 ||
            uSize.width <= 0 || uSize.height <= 0
        ) return@LaunchedEffect
        val maxScale = when {
            superSize -> {
                oSize.width.toFloat() / uSize.width.toFloat()
            }

            widthFixed -> {
                bSize.height.toFloat() / uSize.height.toFloat()
            }

            else -> {
                bSize.width.toFloat() / uSize.width.toFloat()
            }
        }
        latestOnSizeChange(
            SizeChangeContent(
                defaultSize = uSize,
                containerSize = bSize,
                maxScale = maxScale
            )
        )
    }

    // 承载容器的透明度，主要用来控制图片加载成功后的渐变效果
    val viewerAlpha = remember { Animatable(0F) }
    // 每个 model 只挂载回调一次；回调与动画窗格始终取最新值
    var mountedModel by remember { mutableStateOf<Any?>(null) }
    val latestOnMounted by rememberUpdatedState(onMounted)
    val latestCrossfadeSpec by rememberUpdatedState(crossfadeAnimationSpec)

    // 模型就绪后每个 model 只淡入/回调一次；动画在 effect 内执行，离开组合或模型变化时随 effect 取消。
    LaunchedEffect(model, imageSpecified) {
        if (imageSpecified && mountedModel != model) {
            viewerAlpha.animateTo(1F, latestCrossfadeSpec)
            mountedModel = model
            latestOnMounted()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // 图片位移时会超出容器大小，需要在这个地方指定是否裁切
                clip = boundClip
                alpha = viewerAlpha.value
            }
            .onSizeChanged {
                bSize = it
            }
            .pointerInput(gesture) {
                detectTapGestures(onLongPress = gesture.onLongPress)
            }
            .pointerInput(imageSpecified, gesture) {
                if (imageSpecified) detectTransformGestures(
                    onTap = gesture.onTap,
                    onDoubleTap = gesture.onDoubleTap,
                    gestureStart = gesture.gestureStart,
                    gestureEnd = gesture.gestureEnd,
                    onGesture = gesture.onGesture,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val imageModifier = Modifier
            .graphicsLayer {
                if (imageSpecified) {
                    // 手势值在 Draw 阶段读取，缩放/拖动不再重组整个页面
                    val s = scale()
                    scaleX = s
                    scaleY = s
                    translationX = offsetX()
                    translationY = offsetY()
                    rotationZ = rotation()
                }
            }
            .size(
                LocalDensity.current.run { uSize.width.toDp() },
                LocalDensity.current.run { uSize.height.toDp() }
            )
        when (model) {
            is Painter -> {
                Image(
                    painter = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier,
                )
            }

            is ImageVector -> {
                Image(
                    imageVector = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier,
                )
            }

            is ImageBitmap -> {
                Image(
                    bitmap = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier,
                )
            }

            is ComposeModel -> {
                Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
                    model.PoseContent()
                }
            }
        }

    }
}