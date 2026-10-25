package dev.aaa1115910.bv.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.aaa1115910.bv.player.danmaku.DanmakuView

/**
 * 弹幕层：把自研弹幕引擎的 [DanmakuView]（普通 View，内部 3 线程 + Choreographer 帧驱动）
 * 挂进组合。View 由调用方创建、配置（[DanmakuView.setConfig] / 数据 / 位置与播放状态
 * provider）并在离开页面时 [DanmakuView.release]；本组件只负责把它放进组合树。
 */
@Composable
fun BvDanmakuPlayer(
    danmakuView: DanmakuView?,
    modifier: Modifier = Modifier,
) {
    if (danmakuView == null) return
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { danmakuView },
    )
}
