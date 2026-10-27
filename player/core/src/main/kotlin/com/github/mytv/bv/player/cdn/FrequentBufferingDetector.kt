package com.github.mytv.bv.player.cdn

/**
 * 「频繁缓冲」判定：时间窗口 [windowMs] 内进入缓冲的次数达到 [maxCount] 次时判定为地址有问题。
 *
 * 起播时那次缓冲（首次 READY 之前）与 seek 后 [seekGraceMs] 内的缓冲都不计入：
 * 前者必然发生，后者是用户主动造成的，都不能说明 CDN 地址有问题。
 * 时间由调用方传入（建议使用单调时钟），便于单测与复用。
 */
internal class FrequentBufferingDetector(
    private val maxCount: Int = 3,
    private val windowMs: Long = 30_000L,
    private val seekGraceMs: Long = 3_000L
) {
    /** 首次进入 READY 之后才开始计数，避免把起播缓冲算进去 */
    private var armed = false
    private var lastSeekAtMs = Long.MIN_VALUE
    private val recentStarts = ArrayDeque<Long>()

    /** 进入 READY（缓冲结束） */
    fun onReady() {
        armed = true
    }

    /** 记录一次用户 seek（快进/快退），随后的短暂缓冲不计入 */
    fun onSeek(nowMs: Long) {
        lastSeekAtMs = nowMs
    }

    /**
     * 进入缓冲。
     *
     * @return true 表示已达到切换阈值，调用方应切到下一个候选地址；
     *   返回 true 时窗口会被清空，避免连续缓冲时反复触发
     */
    fun onBufferingStart(nowMs: Long): Boolean {
        if (!armed) return false
        if (lastSeekAtMs != Long.MIN_VALUE && nowMs - lastSeekAtMs <= seekGraceMs) return false

        recentStarts.addLast(nowMs)
        while (recentStarts.isNotEmpty() && recentStarts.first() < nowMs - windowMs) {
            recentStarts.removeFirst()
        }
        if (recentStarts.size < maxCount) return false
        recentStarts.clear()
        return true
    }

    /** 换地址/重新起播后重置，新一轮的起播缓冲同样不计 */
    fun reset() {
        armed = false
        lastSeekAtMs = Long.MIN_VALUE
        recentStarts.clear()
    }
}
