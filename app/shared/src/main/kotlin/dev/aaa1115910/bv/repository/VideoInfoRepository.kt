package dev.aaa1115910.bv.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
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
    var videoList: List<VideoListItem> by mutableStateOf(emptyList(), referentialEqualityPolicy())
        private set

    /** Publish a complete snapshot; callers cannot mutate the currently displayed list in place. */
    fun replaceVideoList(items: List<VideoListItem>) {
        videoList = items.toList()
    }

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
