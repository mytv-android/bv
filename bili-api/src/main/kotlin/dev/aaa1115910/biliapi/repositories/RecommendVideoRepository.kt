package dev.aaa1115910.biliapi.repositories

import bilibili.app.show.v1.PopularGrpcKt
import bilibili.app.show.v1.popularResultReq
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.home.RecommendData
import dev.aaa1115910.biliapi.entity.home.RecommendPage
import dev.aaa1115910.biliapi.entity.rank.PopularVideoData
import dev.aaa1115910.biliapi.entity.rank.PopularVideoPage
import dev.aaa1115910.biliapi.entity.story.RecommendStoryData
import dev.aaa1115910.biliapi.entity.story.StoryItem
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class RecommendVideoRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val popularStub
        get() = runCatching {
            PopularGrpcKt.PopularCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getPopularVideos(
        page: PopularVideoPage,
        preferApiType: ApiType = ApiType.Web
    ): PopularVideoData {
        return when (preferApiType) {
            ApiType.Web -> {
                val response = BiliHttpApi.getPopularVideoData(
                    pageSize = page.nextWebPageSize,
                    pageNumber = page.nextWebPageNumber,
                    sessData = authRepository.sessionData ?: ""
                ).getResponseData()
                val list = response.list.map { UgcItem.fromVideoInfo(it) }
                val nextPage = PopularVideoPage(
                    nextWebPageSize = page.nextWebPageSize,
                    nextWebPageNumber = page.nextWebPageNumber + 1
                )
                PopularVideoData(
                    list = list,
                    nextPage = nextPage,
                    noMore = response.noMore
                )
            }

            ApiType.App -> {
                val reply = popularStub?.index(popularResultReq {
                    idx = page.nextAppIndex.toLong()
                })
                val list = reply?.itemsList
                    ?.filter { it.itemCase == bilibili.app.card.v1.Card.ItemCase.SMALL_COVER_V5 }
                    ?.map { UgcItem.fromSmallCoverV5(it.smallCoverV5) }
                    ?: emptyList()
                val nextPage = PopularVideoPage(
                    nextAppIndex = list.lastOrNull()?.idx ?: -1
                )
                PopularVideoData(
                    list = list,
                    nextPage = nextPage,
                    noMore = nextPage.nextAppIndex == -1
                )
            }
        }
    }

    suspend fun getRecommendVideos(
        page: RecommendPage = RecommendPage(),
        preferApiType: ApiType = ApiType.Web
    ): RecommendData {
        val items = when (preferApiType) {
            ApiType.Web -> BiliHttpApi.getFeedRcmd(
                idx = page.nextWebIdx,
                sessData = authRepository.sessionData
            )
                .getResponseData().item
                .map { UgcItem.fromRcmdItem(it) }

            ApiType.App -> BiliHttpApi.getFeedIndex(
                idx = page.nextAppIdx,
                accessKey = authRepository.accessToken
            )
                .getResponseData().items
                .filter { it.cardGoto == "av" }
                .map { UgcItem.fromRcmdItem(it) }
        }
        val nextPage = when (preferApiType) {
            ApiType.Web -> RecommendPage(
                nextWebIdx = page.nextWebIdx + 1
            )

            ApiType.App -> RecommendPage(
                nextAppIdx = items.first().idx + 1
            )
        }
        return RecommendData(
            items = items,
            nextPage = nextPage
        )
    }

    /**
     * 与 [getRecommendVideos] 相同的首页推荐流，但转换为刷刷使用的 [StoryItem]，保留 cid、UP 主 mid 等
     * 播放和操作需要的字段。供 [StoryRepository] 在 story 接口不可用时回退使用。
     *
     * 推荐流不提供视频宽高，返回项的 dimension 均为 null（按横屏处理）。
     */
    suspend fun getRecommendStoryItems(
        page: RecommendPage = RecommendPage(),
        preferApiType: ApiType = ApiType.Web
    ): RecommendStoryData {
        return when (preferApiType) {
            ApiType.Web -> {
                val items = BiliHttpApi.getFeedRcmd(
                    idx = page.nextWebIdx,
                    sessData = authRepository.sessionData
                ).getResponseData().item
                RecommendStoryData(
                    items = items.mapNotNull { StoryItem.fromWebRcmdItem(it) },
                    nextPage = page.copy(nextWebIdx = page.nextWebIdx + 1)
                )
            }

            ApiType.App -> {
                val items = BiliHttpApi.getFeedIndex(
                    idx = page.nextAppIdx,
                    accessKey = authRepository.accessToken
                ).getResponseData().items
                RecommendStoryData(
                    items = items.mapNotNull { StoryItem.fromAppRcmdItem(it) },
                    nextPage = page.copy(
                        nextAppIdx = items.firstOrNull()?.idx?.plus(1) ?: page.nextAppIdx
                    )
                )
            }
        }
    }
}
