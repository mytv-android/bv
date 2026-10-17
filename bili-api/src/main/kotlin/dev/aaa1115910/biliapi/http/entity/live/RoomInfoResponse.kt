package dev.aaa1115910.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播间完整信息响应。
 *
 * 端点: `GET /room/v1/Room/get_info?room_id={roomId}`
 *
 * 注意: 该接口已不再返回 uname/face 字段，
 * 如需主播昵称需另行调用 `/live_user/v1/Master/info?uid=` 查询。
 */
@Serializable
data class RoomInfoData(
    @SerialName("room_id")
    val roomId: Int = 0,
    @SerialName("short_id")
    val shortId: Int = 0,
    val uid: Long = 0,
    @SerialName("live_status")
    val liveStatus: Int = 0,
    @SerialName("live_time")
    val liveTime: String = "",
    @SerialName("live_day_count")
    val liveDayCount: Int = 0,
    val title: String = "",
    val description: String = "",
    val tags: String = "",
    val background: String = "",
    val uname: String = "",
    val face: String = "",
    val cover: String = "",
    val keyframe: String = "",
    val online: Int = 0,
    val area: Int = 0,
    @SerialName("area_name")
    val areaName: String = "",
    @SerialName("area_v2_id")
    val areaV2Id: Int = 0,
    @SerialName("area_v2_name")
    val areaV2Name: String = "",
    @SerialName("area_v2_parent_id")
    val areaV2ParentId: Int = 0,
    @SerialName("area_v2_parent_name")
    val areaV2ParentName: String = "",
    @SerialName("parent_area_id")
    val parentAreaId: Int = 0,
    @SerialName("parent_area_name")
    val parentAreaName: String = "",
    @SerialName("watched_show")
    val watchedShow: WatchedShow? = null,
    @SerialName("is_strict_room")
    val isStrictRoom: Boolean = false,
    @SerialName("is_hidden")
    val isHidden: Boolean = false,
    @SerialName("is_locked")
    val isLocked: Boolean = false,
    @SerialName("is_portrait")
    val isPortrait: Boolean = false,
    @SerialName("is_sp")
    val isSp: Int = 0,
    @SerialName("special_type")
    val specialType: Int = 0
)
