package dev.aaa1115910.biliapi.entity.video

import dev.aaa1115910.biliapi.http.entity.video.InteractiveEdgeInfo

/**
 * 互动视频的一个节点：既是选项（choice，普通节点的待选项），
 * 也是故事节点（story node，已走过的路径，可用于回退到上一节点）。
 */
data class InteractiveNode(
    val nodeId: Long,
    val edgeId: Long? = null,
    val cid: Long,
    val title: String,
    val cover: String? = null,
    /** 节点视频的起始位置（秒），回退到该节点时从这里开始播 */
    val startPos: Int? = null,
    val cursor: Int? = null,
    val isCurrent: Boolean = false,
) {
    companion object {
        fun fromStoryNode(node: InteractiveEdgeInfo.StoryNode) = InteractiveNode(
            nodeId = node.nodeId,
            edgeId = node.edgeId,
            cid = node.cid,
            title = node.title,
            cover = node.cover,
            startPos = node.startPos,
            cursor = node.cursor,
            isCurrent = node.isCurrent == 1,
        )

        fun fromChoice(choice: InteractiveEdgeInfo.Choice, fallbackTitle: String) = InteractiveNode(
            nodeId = choice.id,
            edgeId = choice.id,
            cid = choice.cid,
            title = choice.option.ifBlank { fallbackTitle },
            isCurrent = choice.isCurrent == 1,
        )
    }
}
