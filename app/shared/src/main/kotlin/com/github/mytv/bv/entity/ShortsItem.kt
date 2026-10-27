package com.github.mytv.bv.entity

import androidx.compose.runtime.Immutable
import dev.aaa1115910.biliapi.entity.story.StoryItem
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.video.VideoDetail

/**
 * 刷刷里的一条视频（story 流、推荐流回退、视频详情三种来源统一转换成它）
 *
 * 计数字段为 null 表示数据源没有提供（推荐流回退时只有播放、点赞数），界面上应显示文字标签而不是数字。
 *
 * @param cid 为 0 表示未知（只有从 [UgcItem] 转换时会出现，播放前需要先取视频详情）
 * @param width 视频宽度，0 表示未知
 * @param height 视频高度，0 表示未知
 * @param duration 时长，秒
 * @param pubdate 发布时间，unix 秒；未知为 null
 * @param authorFans UP 主粉丝数；未知为 null
 */
@Immutable
data class ShortsItem(
    val aid: Long,
    val cid: Long,
    val bvid: String? = null,
    val title: String,
    val cover: String,
    val author: Author,
    val likeCount: Long? = null,
    val coinCount: Long? = null,
    val favoriteCount: Long? = null,
    val replyCount: Long? = null,
    val shareCount: Long? = null,
    val viewCount: Long? = null,
    val duration: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val tags: List<String> = emptyList(),
    val pubdate: Long? = null,
    val description: String? = null,
    val authorFans: Long? = null
) {
    @Immutable
    data class Author(
        val mid: Long,
        val name: String,
        val face: String
    )

    /** 是否已知宽高 */
    val hasDimension: Boolean get() = width > 0 && height > 0

    /** 竖屏视频（高 > 宽）；没有宽高时按横屏处理 */
    val isPortrait: Boolean get() = hasDimension && height > width

    companion object {
        fun fromStoryItem(item: StoryItem) = ShortsItem(
            aid = item.aid,
            cid = item.cid,
            bvid = item.bvid,
            title = item.title,
            cover = item.cover,
            author = Author(
                mid = item.author.mid,
                name = item.author.name,
                face = item.author.face
            ),
            likeCount = item.like,
            coinCount = item.coin,
            favoriteCount = item.favorite,
            replyCount = item.reply,
            shareCount = item.share,
            viewCount = item.view,
            duration = item.duration,
            width = item.dimension?.width ?: 0,
            height = item.dimension?.height ?: 0,
            tags = item.tags,
            pubdate = item.pubdate,
            description = item.description,
            authorFans = item.authorFans
        )

        /**
         * 首页推荐卡片（[UgcItem]）没有 cid、宽高和 UP 主 mid / 头像，转换后 cid 为 0
         */
        fun fromUgcItem(item: UgcItem) = ShortsItem(
            aid = item.aid,
            cid = 0,
            bvid = item.bvid.takeIf { it.isNotBlank() },
            title = item.title,
            cover = item.cover,
            author = Author(mid = 0, name = item.author, face = ""),
            viewCount = item.play.takeIf { it >= 0 }?.toLong(),
            duration = item.duration
        )

        /**
         * @param cid 要播放的分 P，默认第一 P
         */
        fun fromVideoDetail(detail: VideoDetail, cid: Long = detail.cid): ShortsItem {
            val page = detail.pages.firstOrNull { it.cid == cid } ?: detail.pages.firstOrNull()
            return ShortsItem(
                aid = detail.aid,
                cid = cid,
                bvid = detail.bvid.takeIf { it.isNotBlank() },
                title = detail.title,
                cover = detail.cover,
                author = Author(
                    mid = detail.author.mid,
                    name = detail.author.name,
                    face = detail.author.face
                ),
                likeCount = detail.stat.like.toLong(),
                coinCount = detail.stat.coin.toLong(),
                favoriteCount = detail.stat.favorite.toLong(),
                replyCount = detail.stat.reply.toLong(),
                shareCount = detail.stat.share.toLong(),
                viewCount = detail.stat.view.toLong(),
                duration = page?.duration ?: 0,
                width = page?.dimension?.width ?: 0,
                height = page?.dimension?.height ?: 0,
                tags = detail.tags.map { it.name },
                pubdate = (detail.publishDate.time / 1000).takeIf { it > 0 },
                description = detail.description.takeIf { it.isNotBlank() }
            )
        }
    }
}
