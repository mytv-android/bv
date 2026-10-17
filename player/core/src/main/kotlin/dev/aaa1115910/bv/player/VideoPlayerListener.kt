package dev.aaa1115910.bv.player

interface VideoPlayerListener {
    /** 异常 */
    fun onError(error: Exception)

    /**
     * 准备
     */
    fun onReady()

    /** 播放 */
    fun onPlay()

    /** 暂停 */
    fun onPause()

    /** 缓冲中 */
    fun onBuffering()

    /** 播放结束 */
    fun onEnd()

    /** 空闲，例如播放前 */
    fun onIdle()

    /** 后退 */
    fun onSeekBack(seekBackIncrementMs: Long)

    /** 前进 */
    fun onSeekForward(seekForwardIncrementMs: Long)

    /**
     * 播放器自动切换了 CDN 候选地址（仅当通过候选列表接口起播且真的发生切换时回调）。
     * 默认空实现，现有回调方无需改动；可用于上报/提示。
     *
     * @param fromHost 切换前的 host，无法解析时为 null
     * @param toHost 切换后的 host，无法解析时为 null
     */
    fun onCdnSwitched(fromHost: String?, toHost: String?) {}

}