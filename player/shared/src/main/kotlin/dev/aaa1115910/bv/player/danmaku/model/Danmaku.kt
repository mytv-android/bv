package dev.aaa1115910.bv.player.danmaku.model

data class Danmaku(
    val dmid: Long,
    val positionMs: Int,
    val text: String,
    val type: Int,
    var convertedType: Int = 0,
    val textSize: Int,
    val color: Int,
    val level: Int = 0,
) : Comparable<Danmaku> {
    override fun compareTo(other: Danmaku): Int = positionMs.compareTo(other.positionMs)

    val mode: Int
        get() = if (this.convertedType != 0) this.convertedType else this.type

    companion object {
        const val MODE_SCROLL = 1
        const val MODE_BOTTOM = 4
        const val MODE_TOP = 5
    }
}
