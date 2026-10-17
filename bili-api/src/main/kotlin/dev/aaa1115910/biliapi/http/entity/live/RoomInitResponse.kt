package dev.aaa1115910.biliapi.http.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 直播间初始化响应（短号→长号转换）。
 *
 * 端点: `GET /room/v1/Room/room_init?id={roomId}`
 */
@Serializable
data class RoomInitData(
    @SerialName("room_id")
    val roomId: Int = 0,
    @SerialName("short_id")
    val shortId: Int = 0,
    val uid: Long = 0,
    @SerialName("need_p2p")
    val needP2P: Int = 0,
    @SerialName("is_hidden")
    val isHidden: Boolean = false,
    @SerialName("is_locked")
    val isLocked: Boolean = false,
    @SerialName("is_portrait")
    val isPortrait: Boolean = false,
    @SerialName("live_status")
    val liveStatus: Int = 0,
    @SerialName("hidden_till")
    val hiddenTill: Int = 0,
    @SerialName("lock_till")
    val lockTill: Int = 0,
    val encrypted: Boolean = false,
    @SerialName("pwd_verified")
    val pwdVerified: Boolean = false,
    @SerialName("live_time")
    val liveTime: Long = 0,
    @SerialName("room_shield")
    val roomShield: Int = 0,
    @SerialName("is_sp")
    val isSp: Int = 0,
    @SerialName("special_type")
    val specialType: Int = 0
)
