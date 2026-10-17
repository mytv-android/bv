package dev.aaa1115910.bv.ui.feature.search

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.biliapi.repositories.VideoActionRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.navigation.SearchRoute
import dev.aaa1115910.bv.util.Partition
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.removeHtmlTags
import dev.aaa1115910.bv.viewmodel.search.SearchInputViewModel
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

/** 结果分类（原型的分类标签：综合 视频 番剧 影视 UP 主）。 */
internal enum class SearchCategory(@StringRes val label: Int) {
    All(R.string.search_category_all),
    Video(R.string.search_category_video),
    Bangumi(R.string.search_category_bangumi),
    Ft(R.string.search_category_ft),
    User(R.string.search_category_user),
}

/** 请求发出后最多等这么久就不再显示「加载中」（ViewModel 失败时只打日志，没有错误状态）。 */
private const val PendingTimeoutMs = 10_000L

/** 默认排序，与 SearchResultViewModel.selectedOrder 的初值一致。 */
internal val DefaultSearchOrder = SearchFilterOrderType.MostClicks

/**
 * 搜索页三端共享的状态：
 * - 输入、联想、历史、热搜来自 [SearchInputViewModel]；
 * - 四类结果来自 [SearchResultViewModel]（数据逻辑与旧电视版 SearchResultScreen 相同：update() 同时请求四类，
 *   loadMore(type) 加载下一页，筛选改变后重新 update()）。
 * 两个 ViewModel 跟随返回栈保留，所以从详情页返回时结果仍在；[editing] / [category] 用 rememberSaveable 保存。
 */
@Stable
internal class SearchUiState(
    val input: SearchInputViewModel,
    val result: SearchResultViewModel,
    private val scope: CoroutineScope,
    editingState: MutableState<Boolean>,
    categoryState: MutableState<SearchCategory>,
) {
    /** 手机 / 平板：true 时显示联想（有输入）或历史 + 热搜（无输入），false 时显示结果。 */
    var editing by editingState

    var category by categoryState

    val apiType: ApiType = runCatching { Prefs.apiType }.getOrDefault(ApiType.Web)

    /** 可选的排序：Web 和 App 接口支持的排序不同。 */
    val orderFilters: List<SearchFilterOrderType> =
        if (apiType == ApiType.Web) SearchFilterOrderType.webFilters else SearchFilterOrderType.allFilters

    /** 时长和分区只有 Web 接口支持；App 接口（gRPC SearchByType）只能排序，这两组置灰。 */
    val fullFilters: Boolean = apiType == ApiType.Web

    /** 当前结果对应的关键词（"" = 还没有搜索过）。 */
    val submitted: String get() = result.keyword

    /** 每类结果「请求发出时」的结果对象；结果对象被替换（请求成功）后就不再是加载中。 */
    private val pending = mutableStateMapOf<SearchType, Any>()

    fun resultOf(type: SearchType): SearchResultViewModel.SearchResult = when (type) {
        SearchType.Video -> result.videoSearchResult
        SearchType.MediaBangumi -> result.mediaBangumiSearchResult
        SearchType.MediaFt -> result.mediaFtSearchResult
        SearchType.BiliUser -> result.biliUserSearchResult
    }

    fun isLoading(type: SearchType): Boolean {
        val base = pending[type] ?: return false
        return resultOf(type) === base
    }

    fun isLoading(category: SearchCategory): Boolean = when (category) {
        SearchCategory.All -> SearchType.entries.any { isLoading(it) }
        SearchCategory.Video -> isLoading(SearchType.Video)
        SearchCategory.Bangumi -> isLoading(SearchType.MediaBangumi)
        SearchCategory.Ft -> isLoading(SearchType.MediaFt)
        SearchCategory.User -> isLoading(SearchType.BiliUser)
    }

    private fun track(type: SearchType) {
        val base = resultOf(type)
        pending[type] = base
        scope.launch {
            delay(PendingTimeoutMs)
            if (pending[type] === base) pending.remove(type)
        }
    }

    fun onInput(text: String) {
        input.keyword = text
        editing = true
    }

    /** 提交搜索：写入历史、切到结果。 */
    fun search(keyword: String, addHistory: Boolean = true) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        input.keyword = kw
        if (addHistory) input.addSearchHistory(kw)
        editing = false
        if (kw != submitted || SearchType.entries.all { resultOf(it).count == 0 }) refresh(kw)
    }

    /** 重新请求四类结果（旧电视版：关键词或筛选变化时 update()）。 */
    fun refresh(keyword: String = submitted) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        result.keyword = kw
        result.update()
        SearchType.entries.forEach(::track)
    }

    fun loadMore(type: SearchType) {
        if (submitted.isEmpty() || isLoading(type)) return
        result.loadMore(type)
        track(type)
    }

    fun loadMore(category: SearchCategory) = when (category) {
        SearchCategory.All, SearchCategory.Video -> loadMore(SearchType.Video)
        SearchCategory.Bangumi -> loadMore(SearchType.MediaBangumi)
        SearchCategory.Ft -> loadMore(SearchType.MediaFt)
        SearchCategory.User -> loadMore(SearchType.BiliUser)
    }

    // ---------- 筛选（旧电视版 SearchResultVideoFilter） ----------

    fun setOrder(order: SearchFilterOrderType) {
        if (result.selectedOrder == order) return
        result.selectedOrder = order
        refresh()
    }

    fun setDuration(duration: SearchFilterDuration) {
        if (result.selectedDuration == duration) return
        result.selectedDuration = duration
        refresh()
    }

    fun setPartition(partition: Partition?) {
        result.selectedPartition = partition
        result.selectedChildPartition = null
        refresh()
    }

    fun setChildPartition(partition: Partition) {
        result.selectedChildPartition = if (partition != result.selectedChildPartition) partition else null
        refresh()
    }

    /** 相对默认值改过的筛选项数（排序 / 时长 / 分区），显示在手机筛选条的「筛选」上。 */
    val filterCount: Int
        get() = listOf(
            result.selectedOrder != DefaultSearchOrder,
            result.selectedDuration != SearchFilterDuration.All,
            result.selectedPartition != null,
        ).count { it }

    fun resetFilters() {
        if (filterCount == 0) return
        result.selectedOrder = DefaultSearchOrder
        result.selectedDuration = SearchFilterDuration.All
        result.selectedPartition = null
        result.selectedChildPartition = null
        refresh()
    }
}

@Composable
internal fun rememberSearchUiState(route: SearchRoute): SearchUiState {
    val input: SearchInputViewModel = koinViewModel()
    val result: SearchResultViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val editing = rememberSaveable { mutableStateOf(route.query.isBlank()) }
    val category = rememberSaveable { mutableStateOf(SearchCategory.All) }
    val state = remember(input, result) { SearchUiState(input, result, scope, editing, category) }
    // 带关键词进入：直接打开结果（只在第一次进入时；从详情返回时 ViewModel 里已有结果）
    LaunchedEffect(Unit) {
        if (route.query.isNotBlank() && result.keyword.isEmpty()) state.search(route.query)
    }
    return state
}

/** 输入变化时刷新联想（旧界面在关键词变化时 updateSuggests()；这里加 200ms 防抖）。 */
@Composable
internal fun SuggestEffect(state: SearchUiState) {
    val keyword = state.input.keyword
    LaunchedEffect(keyword) {
        if (keyword.isBlank()) {
            state.input.suggests.clear()
            return@LaunchedEffect
        }
        delay(200)
        state.input.updateSuggests()
    }
}

// ---------- 关注 UP 主 ----------

/** UP 主结果的关注按钮状态：登录后按需查询是否已关注，点击切换。 */
@Stable
internal class FollowState(
    private val repo: VideoActionRepository,
    private val scope: CoroutineScope,
    private val context: Context,
    private val apiType: ApiType,
) {
    private val states = mutableStateMapOf<Long, Boolean>()
    private val checking = mutableSetOf<Long>()
    private val busy = mutableSetOf<Long>()

    val available: Boolean get() = repo.isLoggedIn

    fun followed(mid: Long): Boolean = states[mid] == true

    fun ensure(mid: Long) {
        if (!repo.isLoggedIn || states.containsKey(mid) || !checking.add(mid)) return
        scope.launch {
            val followed = withContext(Dispatchers.IO) {
                runCatching { repo.checkFollowing(mid, apiType) }.getOrNull()
            }
            if (followed != null) states[mid] = followed
            checking.remove(mid)
        }
    }

    fun toggle(mid: Long) {
        if (!busy.add(mid)) return
        val follow = states[mid] != true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (follow) repo.followUser(mid, apiType) else repo.unfollowUser(mid, apiType)
                }.getOrDefault(false)
            }
            if (ok) states[mid] = follow
            Toast.makeText(
                context,
                when {
                    !ok -> R.string.search_follow_failed
                    follow -> R.string.search_follow_done
                    else -> R.string.search_unfollow_done
                },
                Toast.LENGTH_SHORT
            ).show()
            busy.remove(mid)
        }
    }
}

@Composable
internal fun rememberFollowState(apiType: ApiType): FollowState {
    val repo: VideoActionRepository = koinInject()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return remember(repo) { FollowState(repo, scope, context, apiType) }
}

// ---------- 结果 → 卡片数据 ----------

internal fun SearchTypeResult.Video.toCard() = VideoCardData(
    avid = aid,
    title = title.removeHtmlTags(),
    cover = cover,
    upName = author,
    play = play,
    danmaku = danmaku,
    time = duration * 1000L,
    pubTime = pubDate,
)

internal data class PgcItem(
    val seasonId: Int,
    val title: String,
    val cover: String,
    val rating: String?,
    val bangumi: Boolean,
)

internal fun SearchTypeResult.Pgc.toItem(bangumi: Boolean) = PgcItem(
    seasonId = seasonId,
    title = title.removeHtmlTags(),
    cover = cover,
    rating = if (star > 0f) String.format(Locale.getDefault(), "%.1f", star) else null,
    bangumi = bangumi,
)

internal data class UserItem(
    val mid: Long,
    val name: String,
    val avatar: String,
    val sign: String,
)

internal fun SearchTypeResult.User.toItem() = UserItem(
    mid = mid,
    name = name.removeHtmlTags(),
    avatar = avatar,
    sign = sign,
)

@Composable
internal fun SearchUiState.videoCards(): List<VideoCardData> {
    val list = result.videoSearchResult.videos
    return remember(list) { list.map { it.toCard() } }
}

@Composable
internal fun SearchUiState.bangumiItems(): List<PgcItem> {
    val list = result.mediaBangumiSearchResult.mediaBangumis
    return remember(list) { list.map { it.toItem(bangumi = true) } }
}

@Composable
internal fun SearchUiState.ftItems(): List<PgcItem> {
    val list = result.mediaFtSearchResult.mediaFts
    return remember(list) { list.map { it.toItem(bangumi = false) } }
}

@Composable
internal fun SearchUiState.userItems(): List<UserItem> {
    val list = result.biliUserSearchResult.biliUsers
    return remember(list) { list.map { it.toItem() } }
}

@StringRes
internal fun SearchFilterOrderType.labelRes(): Int = when (this) {
    SearchFilterOrderType.ComprehensiveSort -> R.string.search_order_default
    SearchFilterOrderType.MostClicks -> R.string.search_order_click
    SearchFilterOrderType.LatestPublish -> R.string.search_order_pubdate
    SearchFilterOrderType.MostDanmaku -> R.string.search_order_danmaku
    SearchFilterOrderType.MostFavorites -> R.string.search_order_favorite
    SearchFilterOrderType.MostComment -> R.string.search_order_comment
    SearchFilterOrderType.MostLikes -> R.string.search_order_like
}

@StringRes
internal fun SearchFilterDuration.labelRes(): Int = when (this) {
    SearchFilterDuration.All -> R.string.search_duration_all
    SearchFilterDuration.LessThan10Minutes -> R.string.search_duration_lt10
    SearchFilterDuration.Between10And30Minutes -> R.string.search_duration_10_30
    SearchFilterDuration.Between30And60Minutes -> R.string.search_duration_30_60
    SearchFilterDuration.MoreThan60Minutes -> R.string.search_duration_gt60
}

// ---------- 快到底部时加载更多（最后可见项索引 > 总数 − 6） ----------

@Composable
internal fun LazyGridState.OnNearEnd(onLoadMore: () -> Unit) {
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
internal fun LazyListState.OnNearEnd(onLoadMore: () -> Unit) {
    val latest by rememberUpdatedState(onLoadMore)
    LaunchedEffect(this) {
        snapshotFlow {
            val info = layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (info.totalItemsCount > 0 && last > info.totalItemsCount - 6) to info.totalItemsCount
        }.distinctUntilChanged().filter { it.first }.collect { latest() }
    }
}
