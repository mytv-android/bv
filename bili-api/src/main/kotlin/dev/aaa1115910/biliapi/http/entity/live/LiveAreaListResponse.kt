package dev.aaa1115910.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播两级分区列表响应。
 *
 * 端点: `GET /room/v1/area/getList`
 */
@Serializable
data class LiveAreaListResponse(
    val data: List<LiveAreaParent> = emptyList()
)

@Serializable
data class LiveAreaParent(
    val id: Int = 0,
    val name: String = "",
    val list: List<LiveAreaChild> = emptyList()
)

@Serializable
data class LiveAreaChild(
    val id: Int = 0,
    val name: String = "",
    @SerialName("parent_id")
    val parentId: Int = 0,
    @SerialName("parent_name")
    val parentName: String = "",
    val pic: String = "",
    @SerialName("area_type")
    val areaType: Int = 0,
    @SerialName("hot_status")
    val hotStatus: Int = 0
)

/**
 * 分区直播分页列表结果。
 *
 * 由 `GET /room/v1/Area/getRoomList` 返回的数组封装而成。
 * 该端点返回 `data` 直接为数组，不包含 `has_more` 字段，
 * 因此通过返回数量是否达到 `pageSize` 来判断是否还有更多。
 */
data class AreaLiveListResult(
    val list: List<LiveRoomItem>,
    val hasMore: Boolean
)
