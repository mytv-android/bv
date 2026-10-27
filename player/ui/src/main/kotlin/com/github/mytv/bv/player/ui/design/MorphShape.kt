@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package com.github.mytv.bv.player.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.min

/**
 * 圆角从 [from] 过渡到「全圆」（短边的一半）。[t] = 0 时是 [from]，1 时是全圆。
 * 直接对 999dp 做动画时，可见的变化只发生在动画最开始的一瞬间，所以用比例插值。
 */
private class MorphCornerSize(private val from: Dp, private val t: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float {
        val start = with(density) { from.toPx() }
        val end = min(shapeSize.width, shapeSize.height) / 2f
        return start + (end - start) * t
    }
}

internal fun morphToPillShape(from: Dp, t: Float): Shape = RoundedCornerShape(MorphCornerSize(from, t))

/**
 * 「选中 / 播放中」时变成全圆的形状，形状变化用 [spec]（默认 defaultSpatialSpec）。
 * 「减少动效」时直接跳到终态。
 */
@Composable
internal fun animatedPillShape(
    from: Dp,
    pill: Boolean,
    fast: Boolean = false,
    label: String = "pillShape",
): Shape {
    val reduceMotion = LocalPlayerReduceMotion.current
    val spec = when {
        reduceMotion -> snap()
        fast -> MaterialTheme.motionScheme.fastSpatialSpec()
        else -> MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    }
    val t by animateFloatAsState(if (pill) 1f else 0f, spec, label = label)
    return morphToPillShape(from, t)
}
