package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.AreaLiveListResult
import dev.aaa1115910.biliapi.http.entity.live.FollowLiveResponse
import dev.aaa1115910.biliapi.http.entity.live.LiveAreaParent
import dev.aaa1115910.biliapi.http.entity.live.LiveListResponse
import dev.aaa1115910.biliapi.http.entity.live.LiveRecommendResponse
import dev.aaa1115910.biliapi.http.entity.live.PlayCodec
import dev.aaa1115910.biliapi.http.entity.live.RoomInfoData
import dev.aaa1115910.biliapi.http.entity.live.RoomPlayInfoV2Data
import org.koin.core.annotation.Single

/**
 * 直播数据仓库。
 *
 * 聚合 B 站直播 Web 端 API，提供首页推荐、分区列表、直播间信息、流地址获取等能力。
 */
@Single
class LiveRepository(
    private val authRepository: AuthRepository
) {
    /**
     * 获取直播首页模块化列表（分区入口 + 推荐模块 + Banner）
     */
    suspend fun getHome(): LiveListResponse = BiliLiveHttpApi.getLiveList().getResponseData()

    /**
     * 获取推荐直播间列表
     */
    suspend fun getRecommend(): LiveRecommendResponse = BiliLiveHttpApi.getLiveRecommend().getResponseData()

    /**
     * 获取用户关注的主播正在直播的房间列表
     */
    suspend fun getFollowing(): FollowLiveResponse =
        BiliLiveHttpApi.getFollowLive(authRepository.sessionData).getResponseData()

    /**
     * 获取两级直播分区列表
     */
    suspend fun getAreaList(): List<LiveAreaParent> = BiliLiveHttpApi.getLiveAreaList().getResponseData()

    /**
     * 获取分区直播分页列表
     *
     * @param parentAreaId 父分区 ID
     * @param areaId 子分区 ID（0 = 全部）
     * @param page 页码，从 1 开始
     */
    suspend fun getAreaRooms(parentAreaId: Int, areaId: Int, page: Int): AreaLiveListResult {
        val pageSize = 30
        val list = BiliLiveHttpApi.getAreaLiveList(parentAreaId, areaId, page, pageSize).getResponseData()
        return AreaLiveListResult(list = list, hasMore = list.size >= pageSize)
    }

    /**
     * 获取直播间完整信息（标题/封面/在线人数/分区等）
     */
    suspend fun getRoomInfo(roomId: Int): RoomInfoData = BiliLiveHttpApi.getRoomInfo(roomId).getResponseData()

    /**
     * 获取直播流播放信息（多线路 + 可用画质）。
     *
     * 内部流程：短号→长号（room_init）→ v2 流地址接口，
     * v2 失败时降级旧 getRoomPlayInfo / getLiveStreamUrl，仅返回单线路。
     *
     * @param roomId 直播间号（短号或长号）
     * @param qn 画质（0 = 自动）
     */
    suspend fun getRoomPlayInfo(roomId: Int, qn: Int = 0): LivePlayInfo {
        val realRoomId = runCatching {
            BiliLiveHttpApi.getRoomInit(roomId).getResponseData().roomId
        }.getOrDefault(roomId)
        return runCatching {
            resolvePlayInfo(
                BiliLiveHttpApi.getRoomPlayInfoV2(realRoomId, qn, authRepository.sessionData)
                    .getResponseData()
            )
        }.getOrElse {
            // v2 失败时降级旧接口
            val playUrl = BiliLiveHttpApi.getLiveRoomPlayInfo(realRoomId).getResponseData().playUrl
                ?: BiliLiveHttpApi.getLiveStreamUrl(realRoomId, qn).getResponseData()
                    .durl.firstOrNull()?.url
            LivePlayInfo(
                currentQn = 0,
                qualities = emptyList(),
                lines = playUrl?.let { listOf(LivePlayLine(order = 1, url = it)) } ?: emptyList()
            )
        }
    }

    /**
     * 从 v2 流地址响应中解析出可用画质与多条线路。
     *
     * 策略：优先 http_stream（FLV），fallback 到 http_hls，两种协议内均优先 AVC 编码，
     * 并枚举所选编码下的全部 url_info 生成线路。
     */
    internal fun resolvePlayInfo(data: RoomPlayInfoV2Data): LivePlayInfo {
        val playUrl = data.playUrlInfo.playUrl
        val qnDescMap = playUrl.qnDesc.associate { it.qn to it.desc }
        val acceptQns = playUrl.stream
            .flatMap { it.format }
            .flatMap { it.codec }
            .flatMap { it.acceptQn }
            .distinct()
        val qualities = acceptQns.sortedByDescending { it }.mapNotNull { qn ->
            qnDescMap[qn]?.let { desc -> qn to desc }
        }

        val codec = pickBestCodec(data)
            ?: return LivePlayInfo(currentQn = 0, qualities = qualities, lines = emptyList())

        // 同一编码下的每个 url_info 是一条独立 CDN 线路
        val lines = codec.urlInfo
            .mapNotNull { info -> (info.host + codec.baseUrl + info.extra).takeIf { it.isNotBlank() } }
            .distinct()
            .mapIndexed { index, url -> LivePlayLine(order = index + 1, url = url) }

        return LivePlayInfo(currentQn = codec.currentQn, qualities = qualities, lines = lines)
    }

    /**
     * 选择最佳编码。
     *
     * 优先级：http_stream/flv → http_stream/fmp4 → http_stream/ts →
     * http_hls/fmp4 → http_hls/ts → http_hls/flv，每个格式均优先 AVC。
     */
    private fun pickBestCodec(data: RoomPlayInfoV2Data): PlayCodec? {
        val candidates = listOf(
            "http_stream" to listOf("flv", "fmp4", "ts"),
            "http_hls" to listOf("fmp4", "ts", "flv")
        )
        for ((protocol, formats) in candidates) {
            val stream = data.playUrlInfo.playUrl.stream.find { it.protocolName == protocol } ?: continue
            for (formatName in formats) {
                val format = stream.format.find { it.formatName == formatName } ?: continue
                val codec = format.codec.find { it.codecName == "avc" } ?: format.codec.firstOrNull()
                if (codec != null) return codec
            }
        }
        val firstStream = data.playUrlInfo.playUrl.stream.firstOrNull() ?: return null
        return firstStream.format.firstOrNull()?.codec?.firstOrNull()
    }
}

/**
 * 直播线路。
 *
 * @param order 线路序号（从 1 开始，与 UI 展示一致）
 * @param url 完整流地址（host + base_url + extra）
 */
data class LivePlayLine(
    val order: Int,
    val url: String
)

/**
 * 直播流播放信息。
 *
 * @param currentQn 当前实际画质 qn
 * @param qualities 直播间可用画质列表（qn, 描述），按 qn 降序
 * @param lines 同一编码下的多条 CDN 线路，供用户手动切换
 */
data class LivePlayInfo(
    val currentQn: Int,
    val qualities: List<Pair<Int, String>>,
    val lines: List<LivePlayLine>
)
