package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 播放器信息（`/x/player/v2` 响应 data），这里只取互动视频需要的字段：
 * 互动视频才会返回 [interaction]，其中 graph_version 是 stein 节点图的版本号。
 */
@Serializable
data class VideoPlayerInfo(
    val interaction: Interaction? = null,
    @SerialName("last_play_time")
    val lastPlayTime: Int = 0,
    @SerialName("last_play_cid")
    val lastPlayCid: Long = 0L,
) {
    @Serializable
    data class Interaction(
        @SerialName("graph_version")
        val graphVersion: Int? = null,
    )
}
