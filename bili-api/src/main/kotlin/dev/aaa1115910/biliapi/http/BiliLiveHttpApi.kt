package dev.aaa1115910.biliapi.http

import dev.aaa1115910.biliapi.http.entity.BiliResponse
import dev.aaa1115910.biliapi.http.entity.live.DanmuInfoData
import dev.aaa1115910.biliapi.http.entity.live.FollowLiveResponse
import dev.aaa1115910.biliapi.http.entity.live.HistoryDanmaku
import dev.aaa1115910.biliapi.http.entity.live.LiveAreaParent
import dev.aaa1115910.biliapi.http.entity.live.LiveListResponse
import dev.aaa1115910.biliapi.http.entity.live.LiveRecommendResponse
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.http.entity.live.RoomInfoData
import dev.aaa1115910.biliapi.http.entity.live.RoomInitData
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoData
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoV2Data
import dev.aaa1115910.biliapi.http.entity.live.SimplePlayUrlData
import dev.aaa1115910.biliapi.http.plugins.BiliUserAgent
import dev.aaa1115910.biliapi.http.util.encApiSign
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BiliLiveHttpApi {
    private var endPoint: String = ""
    private lateinit var client: HttpClient
    private val logger = KotlinLogging.logger { }

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(ContentNegotiation) {
                json(Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    host = "api.live.bilibili.com"
                    protocol = URLProtocol.HTTPS
                }
                header("Referer", "https://live.bilibili.com/")
                header("Origin", "https://live.bilibili.com")
            }
        }.apply {
            encApiSign()
        }
    }

    /**
     * 获取直播间[roomId]的弹幕连接地址等信息，例如 token。
     *
     * 携带 `type=0` 和 `web_location=444.8`，并走 WBI 签名（见 [encApiSign]），
     * 否则会被风控拦截返回 -352。
     */
    suspend fun getLiveDanmuInfo(roomId: Int): BiliResponse<DanmuInfoData> =
        client.get("/xlive/web-room/v1/index/getDanmuInfo") {
            parameter("id", roomId)
            parameter("type", 0)
            parameter("web_location", "444.8")
        }.body()

    /**
     * 获取直播间[roomId]的信息
     */
    suspend fun getLiveRoomPlayInfo(roomId: Int): BiliResponse<RoomPlayInfoData> =
        client.get("/xlive/web-room/v1/index/getRoomPlayInfo") {
            parameter("room_id", roomId)
        }.body()

    /**
     * 获取直播间[roomId]的历史弹幕
     */
    suspend fun getLiveDanmuHistory(roomId: Int): BiliResponse<HistoryDanmaku> =
        client.get("/xlive/web-room/v1/dM/gethistory") {
            parameter("roomid", roomId)
        }.body()

    /**
     * 获取直播首页模块化列表（分区入口 + 推荐模块 + Banner）。
     */
    suspend fun getLiveList(): BiliResponse<LiveListResponse> =
        client.get("/xlive/web-interface/v1/index/getList") {
            parameter("platform", "web")
        }.body()

    /**
     * 获取推荐直播间列表
     */
    suspend fun getLiveRecommend(): BiliResponse<LiveRecommendResponse> =
        client.get("/xlive/web-interface/v1/webMain/getMoreRecList") {
            parameter("platform", "web")
        }.body()

    /**
     * 获取用户关注的主播正在直播的房间列表，需要[sessData]
     */
    suspend fun getFollowLive(sessData: String? = null): BiliResponse<FollowLiveResponse> =
        client.get("/xlive/web-ucenter/v1/xfetter/GetWebList") {
            parameter("hit_ab", false)
            sessData?.takeIf { it.isNotBlank() }?.let { header("Cookie", "SESSDATA=$it;") }
        }.body()

    /**
     * 获取两级直播分区列表
     */
    suspend fun getLiveAreaList(): BiliResponse<List<LiveAreaParent>> =
        client.get("/room/v1/area/getList").body()

    /**
     * 获取分区直播分页列表
     *
     * @param parentAreaId 父分区 ID
     * @param areaId 子分区 ID（0 = 全部）
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量（1~30）
     * @param sortType 排序方式：`online`=热门, `sort_type_2221`=综合
     */
    suspend fun getAreaLiveList(
        parentAreaId: Int,
        areaId: Int,
        page: Int,
        pageSize: Int = 30,
        sortType: String = "online"
    ): BiliResponse<List<LiveRoomItem>> =
        client.get("/room/v1/Area/getRoomList") {
            parameter("parent_area_id", parentAreaId)
            parameter("area_id", areaId)
            parameter("page", page)
            parameter("page_size", pageSize)
            parameter("sort_type", sortType)
        }.body()

    /**
     * 直播间短号→长号转换
     */
    suspend fun getRoomInit(roomId: Int): BiliResponse<RoomInitData> =
        client.get("/room/v1/Room/room_init") {
            parameter("id", roomId)
        }.body()

    /**
     * 获取直播间完整信息（标题/封面/在线人数/分区等）
     */
    suspend fun getRoomInfo(roomId: Int): BiliResponse<RoomInfoData> =
        client.get("/room/v1/Room/get_info") {
            parameter("room_id", roomId)
        }.body()

    /**
     * 获取直播流地址 v2（多协议多画质）
     *
     * @param roomId 真实房间号（长号）
     * @param qn 画质（0 = 自动，10000 = 原画）
     * @param sessData 登录凭证，传入后可获取更高画质
     */
    suspend fun getRoomPlayInfoV2(
        roomId: Int,
        qn: Int = 0,
        sessData: String? = null
    ): BiliResponse<RoomPlayInfoV2Data> =
        client.get("/xlive/web-room/v2/index/getRoomPlayInfo") {
            parameter("room_id", roomId)
            parameter("protocol", "0,1")
            parameter("format", "0,1,2")
            parameter("codec", "0,1,2")
            parameter("qn", qn)
            parameter("platform", "web")
            parameter("ptype", 8)
            sessData?.takeIf { it.isNotBlank() }?.let { header("Cookie", "SESSDATA=$it;") }
        }.body()

    /**
     * 获取简单单画质流地址（fallback）
     *
     * @param cid 真实房间号（长号）
     * @param qn 画质
     */
    suspend fun getLiveStreamUrl(cid: Int, qn: Int = 0): BiliResponse<SimplePlayUrlData> =
        client.get("/room/v1/Room/playUrl") {
            parameter("cid", cid)
            parameter("platform", "web")
            parameter("qn", qn)
        }.body()

}
