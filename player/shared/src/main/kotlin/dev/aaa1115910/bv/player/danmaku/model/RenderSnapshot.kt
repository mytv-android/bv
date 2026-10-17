package dev.aaa1115910.bv.player.danmaku.model

internal class RenderSnapshot(
    var positionMs: Double = 0.0,
    initialCapacity: Int = 256,
) {
    var items: Array<DanmakuItem?> = arrayOfNulls(initialCapacity)
        private set
    var yTop: FloatArray = FloatArray(initialCapacity)
        private set

    var count: Int = 0

    fun ensureCapacity(required: Int) {
        if (required <= items.size) return
        val cap = required.coerceAtLeast(items.size * 2 + 8)
        items = arrayOfNulls(cap)
        yTop = FloatArray(cap)
    }

    fun clear() {
        for (i in 0 until count) items[i] = null
        count = 0
        positionMs = 0.0
    }
}
