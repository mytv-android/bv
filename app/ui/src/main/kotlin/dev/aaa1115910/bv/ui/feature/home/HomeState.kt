package dev.aaa1115910.bv.ui.feature.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.biliapi.entity.ugc.region.UgcFeedPage
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.biliapi.repositories.VideoActionRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.component.FullPageLoading
import dev.aaa1115910.bv.ui.designsystem.component.PageMessage
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import dev.aaa1115910.bv.viewmodel.search.SearchInputViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcAiViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcAnimalViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcCarViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcCinephileViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcDanceViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcDougaViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcEmotionViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcEntViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcFashionViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcFoodViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcGameViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcGymViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcHandmakeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcHealthViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcHomeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcInformationViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcKichikuViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcKnowledgeViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcLifeExperienceViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcLifeJoyViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcMusicViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcMysticismViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcOutdoorsViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcPaintingViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcParentingViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcRuralViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcShortplayViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcSportsViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcTechViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcTravelViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcViewModel
import dev.aaa1115910.bv.viewmodel.ugc.UgcVlogViewModel
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** 首页分区页的 31 个分区（顺序同旧电视版 UgcTopNavItem），默认选第一个「动画」。 */
internal val HomeRegions: List<UgcTypeV2> = listOf(
    UgcTypeV2.Douga, UgcTypeV2.Game, UgcTypeV2.Kichiku, UgcTypeV2.Music, UgcTypeV2.Dance,
    UgcTypeV2.Cinephile, UgcTypeV2.Ent, UgcTypeV2.Knowledge, UgcTypeV2.Tech, UgcTypeV2.Information,
    UgcTypeV2.Food, UgcTypeV2.Shortplay, UgcTypeV2.Car, UgcTypeV2.Fashion, UgcTypeV2.Sports,
    UgcTypeV2.Animal, UgcTypeV2.Vlog, UgcTypeV2.Painting, UgcTypeV2.Ai, UgcTypeV2.Home,
    UgcTypeV2.Outdoors, UgcTypeV2.Gym, UgcTypeV2.Handmake, UgcTypeV2.Travel, UgcTypeV2.Rural,
    UgcTypeV2.Parenting, UgcTypeV2.Health, UgcTypeV2.Emotion, UgcTypeV2.LifeJoy,
    UgcTypeV2.LifeExperience, UgcTypeV2.Mysticism,
)

/** 平板顶栏的 4 个快捷分区（原型：动画 知识 美食 户外）。 */
internal val TabletQuickRegions: List<UgcTypeV2> =
    listOf(UgcTypeV2.Douga, UgcTypeV2.Knowledge, UgcTypeV2.Food, UgcTypeV2.Outdoors)

@Composable
internal fun rememberRegionNames(): List<String> {
    val context = LocalContext.current
    return remember(context) { HomeRegions.map { it.getDisplayName(context) } }
}

/**
 * 分区 ViewModel：按需创建（旧电视版 UgcContent 一次创建全部 31 个，每个创建时就会发请求）。
 * 存在当前导航项的 ViewModelStore 里，切换分区后仍保留。
 */
@Composable
internal fun regionViewModel(type: UgcTypeV2): UgcViewModel = when (type) {
    UgcTypeV2.Douga -> koinViewModel<UgcDougaViewModel>()
    UgcTypeV2.Game -> koinViewModel<UgcGameViewModel>()
    UgcTypeV2.Kichiku -> koinViewModel<UgcKichikuViewModel>()
    UgcTypeV2.Music -> koinViewModel<UgcMusicViewModel>()
    UgcTypeV2.Dance -> koinViewModel<UgcDanceViewModel>()
    UgcTypeV2.Cinephile -> koinViewModel<UgcCinephileViewModel>()
    UgcTypeV2.Ent -> koinViewModel<UgcEntViewModel>()
    UgcTypeV2.Knowledge -> koinViewModel<UgcKnowledgeViewModel>()
    UgcTypeV2.Tech -> koinViewModel<UgcTechViewModel>()
    UgcTypeV2.Information -> koinViewModel<UgcInformationViewModel>()
    UgcTypeV2.Food -> koinViewModel<UgcFoodViewModel>()
    UgcTypeV2.Shortplay -> koinViewModel<UgcShortplayViewModel>()
    UgcTypeV2.Car -> koinViewModel<UgcCarViewModel>()
    UgcTypeV2.Fashion -> koinViewModel<UgcFashionViewModel>()
    UgcTypeV2.Sports -> koinViewModel<UgcSportsViewModel>()
    UgcTypeV2.Animal -> koinViewModel<UgcAnimalViewModel>()
    UgcTypeV2.Vlog -> koinViewModel<UgcVlogViewModel>()
    UgcTypeV2.Painting -> koinViewModel<UgcPaintingViewModel>()
    UgcTypeV2.Ai -> koinViewModel<UgcAiViewModel>()
    UgcTypeV2.Home -> koinViewModel<UgcHomeViewModel>()
    UgcTypeV2.Outdoors -> koinViewModel<UgcOutdoorsViewModel>()
    UgcTypeV2.Gym -> koinViewModel<UgcGymViewModel>()
    UgcTypeV2.Handmake -> koinViewModel<UgcHandmakeViewModel>()
    UgcTypeV2.Travel -> koinViewModel<UgcTravelViewModel>()
    UgcTypeV2.Rural -> koinViewModel<UgcRuralViewModel>()
    UgcTypeV2.Parenting -> koinViewModel<UgcParentingViewModel>()
    UgcTypeV2.Health -> koinViewModel<UgcHealthViewModel>()
    UgcTypeV2.Emotion -> koinViewModel<UgcEmotionViewModel>()
    UgcTypeV2.LifeJoy -> koinViewModel<UgcLifeJoyViewModel>()
    UgcTypeV2.LifeExperience -> koinViewModel<UgcLifeExperienceViewModel>()
    UgcTypeV2.Mysticism -> koinViewModel<UgcMysticismViewModel>()
    else -> error("Unsupported home region $type")
}

/** 精选视频的补充信息（推荐数据没有简介和分区名，按需取）。 */
internal data class FeaturedExtra(val description: String?, val region: String?)

/** 进程内缓存：精选补充信息、点赞状态。只在主线程写。 */
private object HomeMemory {
    val extras = mutableStateMapOf<Long, FeaturedExtra>()
    val liked = mutableStateMapOf<Long, Boolean>()
}

/** 继续观看的一项：卡片 + 进度（0..1）。 */
internal data class ContinueItem(val card: VideoCardData, val progress: Float)

internal fun UgcItem.toCard() = VideoCardData(
    avid = aid,
    title = title,
    cover = cover,
    upName = author,
    play = play.takeIf { it >= 0 },
    danmaku = danmaku.takeIf { it >= 0 },
    time = duration * 1000L,
)

internal fun DynamicVideo.toCard() = VideoCardData(
    avid = aid,
    title = title,
    cover = cover,
    upName = author,
    play = play.takeIf { it >= 0 },
    danmaku = danmaku.takeIf { it >= 0 },
    time = duration * 1000L,
)

private fun parseClock(text: String): Long? = runCatching {
    text.trim().split(":").fold(0L) { acc, part -> acc * 60 + part.trim().toLong() }
}.getOrNull()

/**
 * HistoryViewModel 的卡片里 timeString 是「看到 / 总时长」（R.string.play_time_history）或「已看完」，
 * 从中算出进度条比例；角标改为只显示总时长。
 */
internal fun VideoCardData.toContinueItem(): ContinueItem {
    val parts = timeString.split(" / ")
    if (parts.size == 2) {
        val watched = parseClock(parts[0])
        val total = parseClock(parts[1])
        if (watched != null && total != null && total > 0) {
            return ContinueItem(copy(timeString = parts[1].trim()), (watched.toFloat() / total).coerceIn(0f, 1f))
        }
    }
    return ContinueItem(this, 1f)
}

/**
 * 首页三端共享的数据与动作（第 9.1 节「数据」）。ViewModel 在 HomeScreen 里创建，三种布局共享同一份。
 *
 * 加载调用照抄旧界面（app/mobile RcmdPage/PopularPage 所在的 HomeScreen、app/tv HomeContent / UgcContent），
 * 区别：旧界面在 Dispatchers.IO 上调用，这里改在 viewModelScope（主线程）上调用——这些 ViewModel 会直接写
 * mutableStateOf / SnapshotStateList，不能在后台线程写；网络请求本身是挂起函数，不会阻塞主线程。
 * 用 viewModelScope 而不是 rememberCoroutineScope，离开首页（打开详情）时正在进行的加载不会被取消。
 */
@Stable
internal class HomeState(
    val recommend: RecommendViewModel,
    val popular: PopularViewModel,
    val history: HistoryViewModel,
    val searchInput: SearchInputViewModel,
    val user: UserViewModel,
    val dynamic: DynamicViewModel,
    private val context: Context,
    private val toViewRepository: ToViewRepository,
    private val videoActionRepository: VideoActionRepository,
    private val authRepository: AuthRepository,
) {
    private val scope get() = recommend.viewModelScope

    /** 精选 = 推荐列表前 5 条。 */
    val featured: List<UgcItem> get() = recommend.recommendVideoList.take(5)

    /** 继续观看 = 历史记录前 10 条；未登录时为空（整个区块隐藏）。 */
    val continueWatching: List<ContinueItem>
        get() = if (!user.isLogin) emptyList() else history.histories.take(10).map { it.toContinueItem() }

    fun extraOf(aid: Long): FeaturedExtra? = HomeMemory.extras[aid]
    fun isLiked(aid: Long): Boolean = HomeMemory.liked[aid] == true

    // ---------- 推荐 ----------

    fun ensureRecommend() {
        if (recommend.recommendVideoList.isEmpty() && !recommend.loading) loadMoreRecommend()
    }

    fun loadMoreRecommend() {
        if (recommend.loading) return
        scope.launch {
            recommend.loadMore()
            recommend.refreshing = false
        }
    }

    /** 下拉刷新指示器的状态（RecommendViewModel.refreshing 初始为 true，首次加载时不应显示下拉指示器）。 */
    var recommendPulling by mutableStateOf(false)
        private set

    fun refreshRecommend() {
        recommendPulling = true
        scope.launch {
            try {
                recommend.resetPage()
                //避免刷新太快
                delay(300)
                recommend.loadMore {
                    //clear data before set new data
                    recommend.clearData()
                }
                recommend.refreshing = false
            } finally {
                recommendPulling = false
            }
        }
    }

    // ---------- 热门 ----------

    fun ensurePopular() {
        if (popular.popularVideoList.isEmpty() && !popular.loading) loadMorePopular()
    }

    fun loadMorePopular() {
        if (popular.loading) return
        scope.launch {
            popular.loadMore()
            popular.refreshing = false
        }
    }

    var popularPulling by mutableStateOf(false)
        private set

    fun refreshPopular() {
        popularPulling = true
        scope.launch {
            try {
                popular.resetPage()
                //避免刷新太快
                delay(300)
                popular.loadMore {
                    //clear data before set new data
                    popular.clearData()
                }
                popular.refreshing = false
            } finally {
                popularPulling = false
            }
        }
    }

    // ---------- 继续观看 ----------

    fun ensureHistory() {
        if (user.isLogin && history.histories.isEmpty() && !history.updating) history.update()
    }

    // ---------- 关注（电视：动态里的视频投稿） ----------

    fun ensureDynamic() {
        if (dynamic.isLogin && dynamic.dynamicVideoList.isEmpty()) loadMoreDynamic()
    }

    fun loadMoreDynamic() {
        if (dynamic.loadingVideo || !dynamic.isLogin || !dynamic.videoHasMore) return
        scope.launch { dynamic.loadMoreVideo() }
    }

    fun refreshDynamic() {
        dynamic.clearVideoData()
        loadMoreDynamic()
    }

    // ---------- 分区 ----------

    fun loadMoreRegion(vm: UgcViewModel) {
        if (vm.updating || !vm.hasMore) return
        vm.viewModelScope.launch { vm.loadMore() }
    }

    /** 同 UgcViewModel.reloadAll()，但在主线程执行，并且不先清空列表（新数据到达时才替换）。 */
    fun refreshRegion(vm: UgcViewModel, onDone: () -> Unit) {
        vm.viewModelScope.launch {
            vm.nextPage = UgcFeedPage()
            vm.hasMore = true
            vm.showCarousel = true
            vm.loadUgcRegionData()
            vm.loadMore()
            onDone()
        }
    }

    // ---------- 精选补充信息（简介、分区名）----------

    /** 推荐数据没有简介和分区名：按需请求视频详情（与旧电视版 VideoInfoScreen 同一个接口）。取不到就留空。 */
    fun loadFeaturedExtra(aid: Long) {
        if (aid <= 0 || HomeMemory.extras.containsKey(aid)) return
        scope.launch {
            val info = runCatching {
                withContext(Dispatchers.IO) {
                    BiliHttpApi.getVideoDetail(av = aid, sessData = authRepository.sessionData ?: "")
                        .getResponseData().view
                }
            }.getOrNull()
            HomeMemory.extras[aid] = FeaturedExtra(
                description = info?.desc?.trim()?.takeIf { it.isNotEmpty() && it != "-" },
                region = info?.tname?.trim()?.takeIf { it.isNotEmpty() },
            )
            if (user.isLogin) {
                val liked = runCatching { withContext(Dispatchers.IO) { videoActionRepository.checkLiked(aid) } }
                    .getOrDefault(false)
                HomeMemory.liked[aid] = liked
            }
        }
    }

    // ---------- 稍后再看、点赞 ----------

    fun addToView(aid: Long) {
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { toViewRepository.addToView(aid) } }
            val text = if (result.isSuccess) context.getString(R.string.home_toview_added)
            else context.getString(R.string.home_toview_failed, result.exceptionOrNull()?.message ?: "")
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleLike(aid: Long) {
        val target = !isLiked(aid)
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { videoActionRepository.likeVideo(aid, like = target) }
            }
            val text = if (result.isSuccess) {
                HomeMemory.liked[aid] = target
                context.getString(if (target) R.string.home_like_done else R.string.home_unlike_done)
            } else {
                context.getString(R.string.home_like_failed, result.exceptionOrNull()?.message ?: "")
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
internal fun rememberHomeState(): HomeState {
    val recommend: RecommendViewModel = koinViewModel()
    val popular: PopularViewModel = koinViewModel()
    val history: HistoryViewModel = koinViewModel()
    val searchInput: SearchInputViewModel = koinViewModel()
    val user: UserViewModel = koinViewModel()
    val dynamic: DynamicViewModel = koinViewModel()
    val toView: ToViewRepository = koinInject()
    val videoAction: VideoActionRepository = koinInject()
    val auth: AuthRepository = koinInject()
    val context = LocalContext.current.applicationContext
    return remember(recommend, popular, history, searchInput, user, dynamic) {
        HomeState(recommend, popular, history, searchInput, user, dynamic, context, toView, videoAction, auth)
    }
}

/** 列表 → 卡片数据，只在列表变化时重新转换。[skip] 跳过开头若干条。 */
@Composable
internal fun <T> rememberCards(list: List<T>, skip: Int = 0, mapper: (T) -> VideoCardData): List<VideoCardData> {
    val latestMapper by rememberUpdatedState(mapper)
    val cards by remember(list, skip) { derivedStateOf { list.drop(skip).map { latestMapper(it) } } }
    return cards
}

/** 顶部搜索框占位词：第一个热搜词，取不到时用「搜索」。 */
@Composable
internal fun HomeState.searchPlaceholder(): String =
    searchInput.hotwords.firstOrNull()?.showName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_search)

/** 列表快到底部（最后可见项索引 > 总数 − 6）时加载更多。总数变化后若仍在底部会再次触发。 */
@Composable
internal fun LazyGridState.LoadMoreEffect(onLoadMore: () -> Unit) {
    val latest by rememberUpdatedState(onLoadMore)
    LaunchedEffect(this) {
        snapshotFlow {
            val info = layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (info.totalItemsCount > 0 && last > info.totalItemsCount - 6) to info.totalItemsCount
        }.distinctUntilChanged().filter { it.first }.collect { latest() }
    }
}

@Composable
internal fun LazyListState.LoadMoreEffect(onLoadMore: () -> Unit) {
    val latest by rememberUpdatedState(onLoadMore)
    LaunchedEffect(this) {
        snapshotFlow {
            val info = layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (info.totalItemsCount > 0 && last > info.totalItemsCount - 6) to info.totalItemsCount
        }.distinctUntilChanged().filter { it.first }.collect { latest() }
    }
}

/**
 * 列表为空时：加载中（以及刚进入的 600ms 内，避免 ViewModel 还没开始加载时闪一下空提示）显示整页加载，
 * 否则显示空提示 + 重试。
 */
@Composable
internal fun EmptyOrLoading(
    loading: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(600)
        settled = true
    }
    Box(modifier) {
        if (loading || !settled) {
            FullPageLoading()
        } else if (message != null) {
            PageMessage(message = message, onRetry = onRetry)
        } else {
            PageMessage(onRetry = onRetry)
        }
    }
}

/** 在有水平内边距的父布局（网格 contentPadding）里让子项左右各外扩 [horizontal]，用于整行横滑列表贴边。 */
internal fun Modifier.fullBleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val extra = horizontal.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.maxWidth + extra * 2,
            maxWidth = constraints.maxWidth + extra * 2,
        )
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}
