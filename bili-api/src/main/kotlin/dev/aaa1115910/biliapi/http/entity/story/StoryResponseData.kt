package dev.aaa1115910.biliapi.http.entity.story

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * App 端竖屏视频流（刷刷）`GET https://app.bilibili.com/x/v2/feed/index/story` 的 data 部分
 *
 * 字段以 2026-10 实际返回为准（未登录时 track_id 为 gateway_fallback_*，每页 4 条，
 * card_goto 均为 vertical_av）。除了 aid/cid 以外全部可空或带默认值，接口改版时不至于整页解析失败。
 *
 * 注意：请求参数 pull 必须是 0/1，传 true/false 会返回 -400。
 */
@Serializable
data class StoryResponseData(
    val items: List<StoryItem> = emptyList(),
    val config: JsonElement? = null
) {
    @Serializable
    data class StoryItem(
        @SerialName("card_goto")
        val cardGoto: String? = null,
        val goto: String? = null,
        val param: String? = null,
        val idx: Int? = null,
        @SerialName("display_id")
        val displayId: Int? = null,
        val bvid: String? = null,
        val title: String? = null,
        val desc: String? = null,
        val cover: String? = null,
        @SerialName("ff_cover")
        val ffCover: String? = null,
        val uri: String? = null,
        /** 秒 */
        val duration: Int? = null,
        /** unix 秒 */
        val pubdate: Long? = null,
        val owner: Owner? = null,
        val stat: Stat? = null,
        val dimension: Dimension? = null,
        @SerialName("player_args")
        val playerArgs: PlayerArgs? = null,
        /** 登录后才有内容（点赞/投币/收藏/关注状态），未登录为 {} */
        @SerialName("req_user")
        val reqUser: JsonObject? = null,
        /** 接口没有单独的 tag 字段，标签来自这里 id == 3 的「频道:xxx」项 */
        @SerialName("dislike_reasons_v2")
        val dislikeReasonsV2: DislikeReasons? = null,
        @SerialName("track_id")
        val trackId: String? = null,
        @SerialName("short_link")
        val shortLink: String? = null,
        @SerialName("sub_title")
        val subTitle: String? = null,
        @SerialName("view_content")
        val viewContent: String? = null,
        val copyright: Int? = null
    ) {
        @Serializable
        data class Owner(
            val mid: Long = 0,
            val name: String = "",
            val face: String = "",
            val fans: Long? = null,
            val attention: Long? = null,
            @SerialName("like_num")
            val likeNum: Long? = null,
            val relation: Relation? = null
        ) {
            @Serializable
            data class Relation(
                val status: Int? = null
            )
        }

        @Serializable
        data class Stat(
            val aid: Long? = null,
            val view: Long = 0,
            val danmaku: Long = 0,
            val reply: Long = 0,
            val favorite: Long = 0,
            val coin: Long = 0,
            val share: Long = 0,
            val like: Long = 0,
            val follow: Long = 0
        )

        @Serializable
        data class Dimension(
            val width: Int = 0,
            val height: Int = 0,
            /** 1 表示宽高需要互换 */
            val rotate: Int = 0
        )

        @Serializable
        data class PlayerArgs(
            val aid: Long = 0,
            val cid: Long = 0,
            val type: String? = null,
            @SerialName("ugc_season_id")
            val ugcSeasonId: Long? = null
        )

        @Serializable
        data class DislikeReasons(
            val title: String? = null,
            val reasons: List<Reason> = emptyList()
        ) {
            @Serializable
            data class Reason(
                val id: Int = 0,
                val mid: Long? = null,
                val rid: Int? = null,
                @SerialName("tag_id")
                val tagId: Long? = null,
                val name: String = ""
            )
        }
    }
}
