package dev.aaa1115910.bv.player

data class VideoPlayerOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val enableFfmpegAudioRenderer: Boolean = false,
    /** 跳过音频静音段；仅 2.5 倍速及以上真正生效，默认关闭 */
    val enableSkipSilence: Boolean = false
)