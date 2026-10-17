package dev.aaa1115910.bv.player.danmaku.model

import android.graphics.Bitmap

internal enum class DanmakuCacheState {
    Init,
    Rendering,
    Rendered,
}

internal class DanmakuItem(
    val data: Danmaku,
) {
    @Volatile var cacheBitmap: Bitmap? = null
    @Volatile var cacheGeneration: Int = -1
    @Volatile var cacheState: DanmakuCacheState = DanmakuCacheState.Init

    // Active state (action thread only)
    var isActive: Boolean = false
    var kind: DanmakuKind = DanmakuKind.SCROLL
    var lane: Int = 0
    var startTimeMs: Int = 0
    var durationMs: Int = 0
    var pxPerMs: Float = 0f
    var textWidthPx: Float = 0f

    var fontSizeMul: Float = 0f

    fun timeMs(): Int = data.positionMs
}
