package dev.aaa1115910.bv.repository

import dev.aaa1115910.bv.player.entity.VideoListItem
import org.koin.core.annotation.Single

/** 互动视频的节点图上下文：详情页加载后写入，播放器的互动状态机读取 */
data class InteractivePlaybackContext(
    val aid: Long,
    val bvid: String,
    val graphVersion: Int,
)

@Single
class VideoInfoRepository {
    val videoList = mutableListOf<VideoListItem>()

    var interactivePlaybackContext: InteractivePlaybackContext? = null

    fun updateInteractivePlaybackContext(aid: Long, bvid: String, graphVersion: Int?) {
        interactivePlaybackContext = if (bvid.isNotBlank() && graphVersion != null) {
            InteractivePlaybackContext(
                aid = aid,
                bvid = bvid,
                graphVersion = graphVersion,
            )
        } else {
            null
        }
    }

    fun clearInteractivePlaybackContext() {
        interactivePlaybackContext = null
    }
}
