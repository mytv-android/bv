package com.github.mytv.bv.player.entity

interface VideoListItem

open class VideoListItemData(
    open val aid: Long,
    open val cid: Long,
    open val epid: Int? = null,
    open val seasonId: Int? = null,
    open val title: String,
    open val index: Int,
) : VideoListItem {
    /** 互动视频分支用的分支名；普通选集没有，恒为空串 */
    open val partTitle: String = ""
}

data class VideoListPart(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val index: Int,
) : VideoListItemData(aid, cid, epid, seasonId, title, index)

data class VideoListUgcEpisode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val index: Int,
) : VideoListItemData(aid, cid, epid, seasonId, title, index)

data class VideoListUgcEpisodeTitle(
    val index: Int,
    val title: String
) : VideoListItem

data class VideoListPgcEpisode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val index: Int,
) : VideoListItemData(aid, cid, epid, seasonId, title, index)

/** 互动视频的分支（当前节点的待选项，或已走过的节点路径，用于回退） */
data class VideoListInteractiveNode(
    override val aid: Long,
    override val cid: Long,
    override val epid: Int? = null,
    override val seasonId: Int? = null,
    override val title: String,
    override val partTitle: String = "",
    override val index: Int,
    val nodeId: Long,
    val edgeId: Long? = null,
    /** 节点视频的起始位置（秒），null 表示从头播 */
    val startPos: Int? = null,
    val isCurrent: Boolean = false,
) : VideoListItemData(aid, cid, epid, seasonId, title, index)