@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.design

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val BottomScrim = Brush.verticalGradient(
    0.6f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.78f),
)

private val TopScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.45f),
    0.22f to Color.Transparent,
)

/**
 * 第 7.9 节 `CinemaScrim`（原型 `.m-scrim-b`），铺满父容器的两层渐变：
 * - 下：从底部 Black α0.78 到 40% 高度处透明；
 * - 上：从顶部 Black α0.45 到 22% 高度处透明。
 */
@Composable
internal fun CinemaScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(BottomScrim)
                drawRect(TopScrim)
            }
    )
}
