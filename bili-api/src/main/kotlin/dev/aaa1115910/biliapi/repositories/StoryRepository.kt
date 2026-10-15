package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.story.StoryData
import dev.aaa1115910.biliapi.entity.story.StoryItem
import dev.aaa1115910.biliapi.entity.story.StoryPage
import dev.aaa1115910.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

/**
 * 刷刷（竖屏视频流）
 *
 * 优先请求 App 端 `GET https://app.bilibili.com/x/v2/feed/index/story`（与
 * [RecommendVideoRepository] 的 App 推荐流相同的签名与 access_key）。未登录、接口报错或返回为空时
 * 回退到首页推荐流（[RecommendVideoRepository.getRecommendStoryItems]），并把竖屏视频排在前面
 * （推荐流没有宽高，全部当横屏，因此保持原顺序）。
 */
@Single
class StoryRepository(
    private val authRepository: AuthRepository,
    private val recommendVideoRepository: RecommendVideoRepository
) {
    /**
     * 获取一页刷刷视频
     *
     * @param page 分页参数，首次传 `StoryPage(aid = 起始视频 aid)` 或 `StoryPage()`，之后传上次返回的
     * [StoryData.nextPage]
     * @param fallbackApiType 回退推荐流时使用的接口类型
     * @param allowAnonymousStory 未登录时也请求 story 接口（接口实测允许匿名，但返回的是通用兜底内容），
     * 默认按设计要求未登录直接回退推荐流
     */
    suspend fun getStories(
        page: StoryPage = StoryPage(),
        fallbackApiType: ApiType = ApiType.Web,
        allowAnonymousStory: Boolean = false
    ): StoryData {
        val accessKey = authRepository.accessToken?.takeIf { it.isNotBlank() }
        if (accessKey != null || allowAnonymousStory) {
            val storyItems = runCatching {
                BiliHttpApi.getFeedStory(
                    aid = page.aid,
                    pull = page.pull,
                    displayId = page.displayId,
                    accessKey = accessKey
                ).getResponseData().items.mapNotNull { StoryItem.fromStoryHttpItem(it) }
            }.onFailure {
                println("get story failed, fallback to recommend: [page=$page, error=$it]")
            }.getOrNull()

            if (!storyItems.isNullOrEmpty()) {
                return StoryData(
                    items = storyItems,
                    nextPage = page.copy(
                        aid = storyItems.last().aid,
                        displayId = page.displayId + 1,
                        pull = false
                    ),
                    isFallback = false
                )
            }
        }

        val recommendData = recommendVideoRepository.getRecommendStoryItems(
            page = page.recommendPage,
            preferApiType = fallbackApiType
        )
        return StoryData(
            // sortedByDescending 是稳定排序：竖屏在前，其余保持推荐流原顺序
            items = recommendData.items.sortedByDescending { it.dimension?.isVertical == true },
            nextPage = page.copy(recommendPage = recommendData.nextPage),
            isFallback = true
        )
    }
}
