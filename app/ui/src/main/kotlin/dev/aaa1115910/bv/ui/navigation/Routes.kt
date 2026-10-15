package dev.aaa1115910.bv.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class ShortsRoute(val startAid: Long? = null)
@Serializable data object PgcRoute
@Serializable data object DynamicRoute
@Serializable data object MeRoute
@Serializable data class SearchRoute(val query: String = "")
@Serializable data class VideoRoute(val aid: Long)
@Serializable data class SeasonRoute(val seasonId: Int? = null, val epId: Int? = null)

/** 参数照抄旧 VideoPlayerV3Activity（电视）/ VideoPlayerActivity（手机）的 Intent extras。 */
@Serializable
data class PlayerRoute(
    val aid: Long,
    val cid: Long = 0,
    val title: String = "",
    val partTitle: String = "",
    val played: Int = 0,
    val fromSeason: Boolean = false,
    val subType: Int = 0,
    val epid: Int = 0,
    val seasonId: Int = 0,
    val isVerticalVideo: Boolean = false,
    val proxyArea: Int = 0,
    val playerIconIdle: String = "",
    val playerIconMoving: String = "",
)

@Serializable data class SpaceRoute(val mid: Long)
@Serializable data class TagRoute(val tagId: Int)
@Serializable data class DynamicDetailRoute(val id: String)
@Serializable data class PgcIndexRoute(val type: Int)
@Serializable data object PgcTimelineRoute
@Serializable data object HistoryRoute
@Serializable data object FavoriteRoute
@Serializable data object ToViewRoute
@Serializable data object FollowingSeasonRoute
@Serializable data object FollowingRoute
@Serializable data object AccountsRoute
@Serializable data object UserLockRoute
@Serializable data object LoginRoute

/** "" = 分类列表；"appearance" "play" "danmaku" "subtitle" "network" "account" "storage" "about" */
@Serializable data class SettingsRoute(val category: String = "")
@Serializable data object LogsRoute
@Serializable data object MediaCodecRoute
@Serializable data object SpeedTestRoute
