package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 互动视频节点信息（`/x/stein/edgeinfo_v2` 响应 data）。
 *
 * 普通节点（有待选项）在 [edges] 的 questions.choices 里给出可选分支；
 * 叶子节点（结局）没有 choices，已走过的节点路径在 [storyList] 里。
 */
@Serializable
data class InteractiveEdgeInfo(
    val title: String? = null,
    @SerialName("edge_id")
    val edgeId: Long? = null,
    @SerialName("story_list")
    val storyList: List<StoryNode> = emptyList(),
    val edges: Edges? = null,
    @SerialName("is_leaf")
    val isLeaf: Int? = null,
) {
    @Serializable
    data class StoryNode(
        @SerialName("node_id")
        val nodeId: Long,
        @SerialName("edge_id")
        val edgeId: Long? = null,
        val title: String = "",
        val cid: Long,
        @SerialName("start_pos")
        val startPos: Int? = null,
        val cover: String? = null,
        @SerialName("is_current")
        val isCurrent: Int? = null,
        val cursor: Int? = null,
    )

    @Serializable
    data class Edges(
        val questions: List<Question> = emptyList(),
    )

    @Serializable
    data class Question(
        val choices: List<Choice> = emptyList(),
    )

    @Serializable
    data class Choice(
        val id: Long,
        val cid: Long,
        val option: String = "",
        @SerialName("is_current")
        val isCurrent: Int? = null,
    )
}
