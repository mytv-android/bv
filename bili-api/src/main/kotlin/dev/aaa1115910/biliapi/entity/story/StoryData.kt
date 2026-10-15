package dev.aaa1115910.biliapi.entity.story

import dev.aaa1115910.biliapi.entity.home.RecommendPage
import dev.aaa1115910.biliapi.entity.user.Author
import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.entity.video.UserActions
import dev.aaa1115910.biliapi.http.entity.home.RcmdIndexData
import dev.aaa1115910.biliapi.http.entity.home.RcmdTopData
import dev.aaa1115910.biliapi.http.entity.story.StoryResponseData
import dev.aaa1115910.biliapi.util.convertStringTimeToSeconds
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * 刷刷（竖屏视频流）的一页数据
 *
 * @param isFallback 为 true 表示这一页来自推荐流回退（未登录 / story 接口出错 / 返回为空）
 */
data class StoryData(
    val items: List<StoryItem>,
    val nextPage: StoryPage,
    val isFallback: Boolean
)

/**
 * 刷刷分页参数
 *
 * @param aid story 接口的 aid 参数：上一页最后一个视频的 aid，首次为空或起始视频 aid
 * @param displayId story 接口的 display_id，从 1 递增
 * @param pull story 接口的 pull，首次 true，之后 false（请求时转为 1/0）
 * @param recommendPage 回退到推荐流时使用的分页
 */
data class StoryPage(
    val aid: Long? = null,
    val displayId: Int = 1,
    val pull: Boolean = true,
    val recommendPage: RecommendPage = RecommendPage()
)

/**
 * 回退推荐流的一页数据（[dev.aaa1115910.biliapi.repositories.RecommendVideoRepository.getRecommendStoryItems]）
 */
data class RecommendStoryData(
    val items: List<StoryItem>,
    val nextPage: RecommendPage
)

enum class StorySource {
    Story, Recommend
}

/**
 * 刷刷里的一条视频。计数字段为 null 表示数据源没有给出该值（推荐流回退时常见）
 *
 * @param dimension 视频宽高，已按 rotate 校正；为 null 表示数据源没有给出宽高（按横屏处理）
 * @param pubdate unix 秒
 * @param userActions 登录后 story 接口 req_user 给出的点赞/投币/收藏状态，没有时为 null
 */
data class StoryItem(
    val aid: Long,
    val cid: Long,
    val bvid: String?,
    val title: String,
    val cover: String,
    val author: Author,
    val authorFans: Long? = null,
    val view: Long? = null,
    val like: Long? = null,
    val coin: Long? = null,
    val favorite: Long? = null,
    val reply: Long? = null,
    val share: Long? = null,
    val danmaku: Long? = null,
    val duration: Int = 0,
    val dimension: Dimension? = null,
    val tags: List<String> = emptyList(),
    val pubdate: Long? = null,
    val description: String? = null,
    val userActions: UserActions? = null,
    val source: StorySource = StorySource.Story
) {
    companion object {
        private val supportedStoryGotos = setOf("vertical_av", "av")

        /**
         * 转换 story 接口的一项；不是普通稿件（广告、直播等）或缺少 aid/cid 时返回 null
         */
        fun fromStoryHttpItem(item: StoryResponseData.StoryItem): StoryItem? {
            val goto = item.cardGoto ?: item.goto
            if (goto != null && goto !in supportedStoryGotos) return null
            val aid = item.playerArgs?.aid?.takeIf { it > 0 }
                ?: item.stat?.aid?.takeIf { it > 0 }
                ?: item.param?.toLongOrNull()
                ?: return null
            val cid = item.playerArgs?.cid?.takeIf { it > 0 } ?: return null
            val dimension = item.dimension
                ?.takeIf { it.width > 0 && it.height > 0 }
                ?.let {
                    if (it.rotate == 1) Dimension(width = it.height, height = it.width)
                    else Dimension(width = it.width, height = it.height)
                }
            val tags = item.dislikeReasonsV2?.reasons
                ?.filter { it.id == 3 && it.tagId != null && it.name.isNotBlank() }
                ?.map { it.name.substringAfter(':').substringAfter('：').trim() }
                ?.filter { it.isNotEmpty() }
                ?.distinct()
                ?: emptyList()
            return StoryItem(
                aid = aid,
                cid = cid,
                bvid = item.bvid?.takeIf { it.isNotBlank() },
                title = item.title ?: "",
                cover = item.cover ?: item.ffCover ?: "",
                author = Author(
                    mid = item.owner?.mid ?: 0,
                    name = item.owner?.name ?: "",
                    face = item.owner?.face ?: ""
                ),
                authorFans = item.owner?.fans,
                view = item.stat?.view,
                like = item.stat?.like,
                coin = item.stat?.coin,
                favorite = item.stat?.favorite,
                reply = item.stat?.reply,
                share = item.stat?.share,
                danmaku = item.stat?.danmaku,
                duration = item.duration ?: 0,
                dimension = dimension,
                tags = tags,
                pubdate = item.pubdate?.takeIf { it > 0 },
                description = item.desc?.takeIf { it.isNotBlank() && it != "-" },
                userActions = item.reqUser?.toUserActions(),
                source = StorySource.Story
            )
        }

        /**
         * 转换 Web 首页推荐流的一项；非稿件（直播、番剧、广告）返回 null。推荐流没有宽高。
         */
        fun fromWebRcmdItem(item: RcmdTopData.RcmdItem): StoryItem? {
            if (item.goto != "av" || item.id <= 0 || item.cid <= 0) return null
            return StoryItem(
                aid = item.id,
                cid = item.cid,
                bvid = item.bvid.takeIf { it.isNotBlank() },
                title = item.title,
                cover = item.pic,
                author = Author(
                    mid = item.owner?.mid ?: 0,
                    name = item.owner?.name ?: "",
                    face = item.owner?.face ?: ""
                ),
                view = item.stat?.view?.toLong(),
                like = item.stat?.like?.toLong(),
                danmaku = item.stat?.danmaku?.toLong(),
                duration = item.duration,
                dimension = null,
                pubdate = item.pubdate.toLong().takeIf { it > 0 },
                source = StorySource.Recommend
            )
        }

        /**
         * 转换 App 首页推荐流的一项；非稿件或缺少 cid 时返回 null。推荐流没有宽高、头像。
         */
        fun fromAppRcmdItem(item: RcmdIndexData.RcmdItem): StoryItem? {
            if (item.cardGoto != "av") return null
            val aid = item.playerArgs?.aid ?: item.args.aid ?: return null
            val cid = item.playerArgs?.cid?.takeIf { it > 0 } ?: return null
            return StoryItem(
                aid = aid,
                cid = cid,
                bvid = null,
                title = item.title ?: "",
                cover = item.cover ?: "",
                author = Author(
                    mid = item.args.upId ?: 0,
                    name = item.args.upName ?: "",
                    face = ""
                ),
                duration = item.playerArgs?.duration
                    ?: item.coverRightText?.convertStringTimeToSeconds()
                    ?: 0,
                dimension = null,
                source = StorySource.Recommend
            )
        }

        private fun JsonObject.toUserActions(): UserActions? {
            if (isEmpty()) return null
            fun flag(key: String): Boolean {
                val primitive = this[key] as? JsonPrimitive ?: return false
                return primitive.booleanOrNull ?: ((primitive.intOrNull ?: 0) > 0)
            }
            return UserActions(
                like = flag("like"),
                favorite = flag("favorite"),
                coin = flag("coin"),
                dislike = flag("dislike")
            )
        }
    }
}
