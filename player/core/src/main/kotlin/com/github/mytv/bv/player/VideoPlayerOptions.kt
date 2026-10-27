package com.github.mytv.bv.player

import com.github.mytv.bv.player.entity.AudioBalanceLevel

data class VideoPlayerOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val enableFfmpegAudioRenderer: Boolean = false,
    /** 跳过音频静音段；仅 2.5 倍速及以上真正生效，默认关闭 */
    val enableSkipSilence: Boolean = false,
    /** 音量均衡挡位；[AudioBalanceLevel.Off] 时不向音频渲染链插入处理器 */
    val audioBalanceLevel: AudioBalanceLevel = AudioBalanceLevel.Off,
    /**
     * 通过候选列表接口起播时，播放失败/频繁缓冲自动切下一个地址。
     * 仅影响新签名 [AbstractVideoPlayer.playUrl] 的列表重载，单地址调用行为不变，默认开启。
     */
    val enableCdnFailover: Boolean = true
)