package com.github.mytv.bv.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.story.StoryPage
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.StoryRepository
import dev.aaa1115910.biliapi.repositories.VideoActionRepository
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import com.github.mytv.bv.entity.ShortsItem
import com.github.mytv.bv.player.entity.Resolution
import com.github.mytv.bv.repository.PlayUrlResolver
import com.github.mytv.bv.util.Prefs
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.fWarn
import com.github.mytv.bv.util.swapList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

/**
 * 刷刷（竖屏视频流）
 *
 * - [start]：有起始视频时先取其详情作为第一条，再接 story 流（story 不可用时自动回退推荐流）
 * - 页面停稳后调用 [updateCurrentIndex]；当前项之后剩余不足 [AUTO_LOAD_THRESHOLD] 条时自动 [loadMore]
 * - 点赞 / 投币 / 收藏 / 关注 / 稍后再看走 [VideoActionRepository]（与视频详情页相同）
 *
 * 所有 Compose 状态只在主线程（viewModelScope 默认的 Main.immediate）写入，网络请求在 IO 线程执行。
 * 操作结果通过 [events] 通知界面（由界面决定提示文案，ViewModel 不直接 toast）。
 */
@KoinViewModel
class ShortsViewModel(
    private val storyRepository: StoryRepository,
    private val videoDetailRepository: VideoDetailRepository,
    private val videoActionRepository: VideoActionRepository,
    private val videoPlayRepository: VideoPlayRepository,
    private val playUrlResolver: PlayUrlResolver
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }

        /** 当前项之后剩余条数少于该值时自动加载更多 */
        const val AUTO_LOAD_THRESHOLD = 3

        /** 连续多少次加载失败或没有新内容后停止自动加载（翻页后重新计数） */
        private const val MAX_AUTO_LOAD_FAILURES = 3
    }

    /** 视频列表（aid 不重复，可直接作为 VerticalPager 的 key） */
    val items: SnapshotStateList<ShortsItem> = mutableStateListOf()

    /** 当前停留的页 */
    var currentIndex by mutableIntStateOf(0)
        private set

    val currentItem: ShortsItem? get() = items.getOrNull(currentIndex)

    /** 是否正在加载下一页 */
    var loading by mutableStateOf(false)
        private set

    /** 首次进入（取起始视频详情 + 第一页）是否还没完成 */
    var initialLoading by mutableStateOf(false)
        private set

    /** 最近一页是否来自推荐流回退 */
    var isFallback by mutableStateOf(false)
        private set

    /** 最近一次加载失败的信息，成功后清空 */
    var loadError by mutableStateOf<String?>(null)
        private set

    /** 每个视频（aid）的操作状态，没有记录时视为 [ShortsActionState] 默认值 */
    val actionStates: SnapshotStateMap<Long, ShortsActionState> = mutableStateMapOf()

    /** 每个 UP 主（mid）的关注状态，没有记录表示未知 */
    val followingStates: SnapshotStateMap<Long, Boolean> = mutableStateMapOf()

    /** 最近一次 [loadFavoriteFolders] 取到的收藏夹，属于视频 [favoriteFoldersAid] */
    val favoriteFolders: SnapshotStateList<FavoriteFolderMetadata> = mutableStateListOf()
    var favoriteFoldersAid by mutableStateOf<Long?>(null)
        private set
    var favoriteFoldersLoading by mutableStateOf(false)
        private set

    private val _events = MutableSharedFlow<ShortsEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ShortsEvent> = _events.asSharedFlow()

    val isLoggedIn: Boolean get() = videoActionRepository.isLoggedIn

    private var started = false
    /** 每次 [start] 加一，旧任务的收尾不再改动新一轮的状态 */
    private var generation = 0
    private var startAid: Long? = null
    private var nextPage = StoryPage()
    private var autoLoadFailures = 0
    private var startJob: Job? = null
    private var loadJob: Job? = null
    private val actionStateRequests = mutableSetOf<Long>()
    private val pendingActions = mutableSetOf<String>()

    fun actionStateOf(aid: Long): ShortsActionState = actionStates[aid] ?: ShortsActionState()

    fun isFollowing(mid: Long): Boolean? = followingStates[mid]

    // ---------------- 列表 ----------------

    /**
     * 进入刷刷。同一个 [startAid] 重复调用不会重新加载（除非 [force]）
     *
     * @param startAid 起始视频，为 null 时直接从 story 流开始
     */
    fun start(startAid: Long? = null, force: Boolean = false) {
        if (started && !force && startAid == this.startAid) return
        logger.fInfo { "Start shorts: [startAid=$startAid, force=$force]" }
        startJob?.cancel()
        loadJob?.cancel()
        val generation = ++this.generation
        started = true
        this.startAid = startAid
        nextPage = StoryPage(aid = startAid)
        autoLoadFailures = 0
        items.clear()
        currentIndex = 0
        loading = false
        loadError = null
        isFallback = false
        actionStates.clear()
        actionStateRequests.clear()
        initialLoading = true

        startJob = viewModelScope.launch {
            try {
                if (startAid != null) {
                    val detail = runCatching {
                        withContext(Dispatchers.IO) {
                            videoDetailRepository.getVideoDetail(
                                aid = startAid,
                                preferApiType = Prefs.apiType
                            )
                        }
                    }.onFailure {
                        if (it is CancellationException) throw it
                        logger.fWarn { "Load start video av$startAid failed: ${it.stackTraceToString()}" }
                    }.getOrNull()
                    if (detail != null && !detail.redirectToEp) {
                        items.add(ShortsItem.fromVideoDetail(detail))
                        onCurrentItemChanged()
                    }
                }
                loadPage(auto = false)
            } finally {
                if (generation == this@ShortsViewModel.generation) initialLoading = false
            }
        }
    }

    /** 重新从头加载（保留起始视频） */
    fun refresh() = start(startAid = startAid, force = true)

    /** 加载下一页；正在加载（含首次进入）时忽略 */
    fun loadMore() {
        if (!started || loading || initialLoading) return
        loadJob = viewModelScope.launch { loadPage(auto = false) }
    }

    /**
     * 页面停稳后调用。会加载当前视频的操作状态，并在剩余不足 [AUTO_LOAD_THRESHOLD] 条时自动加载更多
     */
    fun updateCurrentIndex(index: Int) {
        if (items.isEmpty()) return
        val newIndex = index.coerceIn(0, items.lastIndex)
        if (newIndex == currentIndex) return
        currentIndex = newIndex
        autoLoadFailures = 0
        onCurrentItemChanged()
        checkAutoLoadMore()
    }

    private suspend fun loadPage(auto: Boolean) {
        if (loading) return
        val generation = this.generation
        loading = true
        try {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    storyRepository.getStories(
                        page = nextPage,
                        fallbackApiType = Prefs.apiType
                    )
                }
            }
            result.onSuccess { data ->
                nextPage = data.nextPage
                isFallback = data.isFallback
                loadError = null
                val existingAids = items.mapTo(HashSet()) { it.aid }
                val newItems = data.items
                    .map { ShortsItem.fromStoryItem(it) }
                    .filter { existingAids.add(it.aid) }
                val wasEmpty = items.isEmpty()
                items.addAll(newItems)
                autoLoadFailures = if (newItems.isEmpty()) autoLoadFailures + 1 else 0
                logger.fInfo { "Load shorts page: [new=${newItems.size}, total=${items.size}, fallback=${data.isFallback}, auto=$auto]" }
                if (wasEmpty && items.isNotEmpty()) onCurrentItemChanged()
            }.onFailure {
                if (it is CancellationException) throw it
                autoLoadFailures++
                loadError = it.localizedMessage ?: it.toString()
                logger.fWarn { "Load shorts page failed: ${it.stackTraceToString()}" }
                _events.tryEmit(ShortsEvent.LoadFailed(it.localizedMessage))
            }
        } finally {
            if (generation == this.generation) loading = false
        }
        if (generation == this.generation) checkAutoLoadMore()
    }

    private fun checkAutoLoadMore() {
        if (!started || loading) return
        val remaining = items.size - 1 - currentIndex
        if (remaining < AUTO_LOAD_THRESHOLD && autoLoadFailures < MAX_AUTO_LOAD_FAILURES) {
            loadJob = viewModelScope.launch { loadPage(auto = true) }
        }
    }

    private fun onCurrentItemChanged() {
        val item = currentItem ?: return
        loadActionState(item)
    }

    // ---------------- 操作状态 ----------------

    /**
     * 查询视频的点赞 / 投币 / 收藏状态和 UP 主关注状态；未登录时不查询。已查过的不重复查询（除非 [force]）
     */
    fun loadActionState(item: ShortsItem, force: Boolean = false) {
        if (!isLoggedIn) return
        val aid = item.aid
        if (!force && actionStates[aid]?.loaded == true) return
        if (!actionStateRequests.add(aid)) return
        viewModelScope.launch {
            try {
                val state = runCatching {
                    withContext(Dispatchers.IO) {
                        videoActionRepository.getActionState(
                            aid = aid,
                            upMid = item.author.mid.takeIf { it > 0 },
                            preferApiType = Prefs.apiType
                        )
                    }
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Load action state of av$aid failed: ${it.stackTraceToString()}" }
                }.getOrNull() ?: return@launch
                actionStates[aid] = actionStateOf(aid).copy(
                    liked = state.liked,
                    coinCount = state.coinCount,
                    favoured = state.favoured,
                    loaded = true
                )
                if (item.author.mid > 0) {
                    state.following?.let { followingStates[item.author.mid] = it }
                }
            } finally {
                actionStateRequests.remove(aid)
            }
        }
    }

    // ---------------- 点赞 ----------------

    /** 点赞 / 取消赞（按钮） */
    fun toggleLike(aid: Long) = setLike(aid, !actionStateOf(aid).liked)

    /** 只点赞不取消（双击、电视长按确认） */
    fun like(aid: Long) {
        if (!actionStateOf(aid).liked) setLike(aid, true)
    }

    private fun setLike(aid: Long, like: Boolean) {
        val action = if (like) ShortsAction.Like else ShortsAction.Unlike
        if (!checkLogin(action)) return
        if (!pendingActions.add("like:$aid")) return
        // 先更新界面（按钮形变动画需要立即响应），失败时回滚
        val before = actionStateOf(aid)
        actionStates[aid] = before.copy(liked = like)
        updateItemCounts(aid, likeDelta = if (like) 1 else -1)
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) { videoActionRepository.likeVideo(aid, like) }
                }.onSuccess {
                    _events.tryEmit(ShortsEvent.ActionSucceeded(action, aid))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "${action.name} av$aid failed: ${it.stackTraceToString()}" }
                    actionStates[aid] = actionStateOf(aid).copy(liked = before.liked)
                    updateItemCounts(aid, likeDelta = if (like) -1 else 1)
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("like:$aid")
            }
        }
    }

    // ---------------- 投币 ----------------

    /**
     * 投币
     *
     * @param multiply 1 或 2
     * @param alsoLike 同时点赞
     */
    fun coin(aid: Long, multiply: Int, alsoLike: Boolean = false) {
        val action = ShortsAction.Coin
        if (!checkLogin(action)) return
        if (multiply !in 1..2) return
        if (!pendingActions.add("coin:$aid")) return
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        videoActionRepository.coinVideo(aid, multiply, alsoLike)
                    }
                }.onSuccess {
                    val before = actionStateOf(aid)
                    val newlyLiked = alsoLike && !before.liked
                    actionStates[aid] = before.copy(
                        coinCount = (before.coinCount + multiply).coerceAtMost(2),
                        liked = before.liked || alsoLike
                    )
                    updateItemCounts(
                        aid,
                        coinDelta = multiply,
                        likeDelta = if (newlyLiked) 1 else 0
                    )
                    _events.tryEmit(ShortsEvent.ActionSucceeded(action, aid))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Coin av$aid failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("coin:$aid")
            }
        }
    }

    // ---------------- 收藏 ----------------

    /** 加载收藏夹列表（用于收藏夹多选对话框），结果在 [favoriteFolders] */
    fun loadFavoriteFolders(aid: Long) {
        if (!checkLogin(ShortsAction.Favorite)) return
        favoriteFoldersAid = aid
        favoriteFoldersLoading = true
        favoriteFolders.clear()
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        videoActionRepository.getFavoriteFolders(aid, Prefs.apiType)
                    }
                }.onSuccess { folders ->
                    if (favoriteFoldersAid != aid) return@onSuccess
                    favoriteFolders.swapList(folders)
                    val favoured = folders.any { it.videoInThisFav }
                    actionStates[aid] = actionStateOf(aid).copy(favoured = favoured)
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Load favorite folders failed: ${it.stackTraceToString()}" }
                    emitFailure(ShortsAction.Favorite, it)
                }
            } finally {
                if (favoriteFoldersAid == aid) favoriteFoldersLoading = false
            }
        }
    }

    /**
     * 把视频放进 [selectedFolderIds] 这些收藏夹，并从 [favoriteFolders] 里其余的收藏夹移除
     * （需先 [loadFavoriteFolders]）
     */
    fun updateFavoriteFolders(aid: Long, selectedFolderIds: List<Long>) {
        val action = ShortsAction.Favorite
        if (!checkLogin(action)) return
        if (favoriteFoldersAid != aid || favoriteFolders.isEmpty()) {
            logger.fWarn { "Favorite folders of av$aid are not loaded" }
            return
        }
        if (!pendingActions.add("fav:$aid")) return
        val allFolderIds = favoriteFolders.map { it.id }
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        videoActionRepository.updateFavoriteFolders(
                            aid = aid,
                            selectedFolderIds = selectedFolderIds,
                            allFolderIds = allFolderIds,
                            preferApiType = Prefs.apiType
                        )
                    }
                }.onSuccess {
                    val before = actionStateOf(aid)
                    val favoured = selectedFolderIds.isNotEmpty()
                    actionStates[aid] = before.copy(favoured = favoured)
                    if (favoured != before.favoured) {
                        updateItemCounts(aid, favoriteDelta = if (favoured) 1 else -1)
                    }
                    if (favoriteFoldersAid == aid) {
                        favoriteFolders.swapList(
                            favoriteFolders.map { it.copy(videoInThisFav = it.id in selectedFolderIds) }
                        )
                    }
                    _events.tryEmit(ShortsEvent.ActionSucceeded(action, aid))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Update favorite folders of av$aid failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("fav:$aid")
            }
        }
    }

    // ---------------- 关注 ----------------

    /** 关注 / 取消关注 UP 主 */
    fun toggleFollow(mid: Long) {
        if (mid <= 0) return
        val following = followingStates[mid] == true
        val action = if (following) ShortsAction.Unfollow else ShortsAction.Follow
        if (!checkLogin(action)) return
        if (!pendingActions.add("follow:$mid")) return
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        if (following) videoActionRepository.unfollowUser(mid, Prefs.apiType)
                        else videoActionRepository.followUser(mid, Prefs.apiType)
                    }
                }.onSuccess { success ->
                    if (success) {
                        followingStates[mid] = !following
                        _events.tryEmit(ShortsEvent.ActionSucceeded(action, mid))
                    } else {
                        emitFailure(action, null)
                    }
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "${action.name} $mid failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("follow:$mid")
            }
        }
    }

    // ---------------- 稍后再看 ----------------

    /** 添加到 / 移出稍后再看（服务端没有便宜的查询接口，状态只记录本次会话里的操作） */
    fun toggleToView(aid: Long) {
        val inToView = actionStateOf(aid).inToView
        val action = if (inToView) ShortsAction.DelToView else ShortsAction.AddToView
        if (!checkLogin(action)) return
        if (!pendingActions.add("toview:$aid")) return
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        if (inToView) videoActionRepository.delToView(aid)
                        else videoActionRepository.addToView(aid)
                    }
                }.onSuccess {
                    actionStates[aid] = actionStateOf(aid).copy(inToView = !inToView)
                    _events.tryEmit(ShortsEvent.ActionSucceeded(action, aid))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "${action.name} av$aid failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("toview:$aid")
            }
        }
    }

    // ---------------- 播放 ----------------

    /**
     * 解析播放地址（供刷刷播放器池使用）
     *
     * @param maxQuality 清晰度上限，移动网络下可传 [PlayUrlResolver.recommendedMaxQuality] 的结果
     */
    suspend fun resolvePlayUrl(
        item: ShortsItem,
        maxQuality: Resolution? = null
    ): PlayUrlResolver.ResolvedPlayUrl = withContext(Dispatchers.IO) {
        playUrlResolver.resolve(aid = item.aid, cid = item.cid, maxQuality = maxQuality)
    }

    /** 上报播放进度（与播放器相同的心跳接口），未登录时忽略 */
    fun reportHistory(item: ShortsItem, timeSeconds: Int) {
        if (!isLoggedIn || item.cid <= 0) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    videoPlayRepository.sendHeartbeat(
                        aid = item.aid,
                        cid = item.cid,
                        time = timeSeconds,
                        preferApiType = Prefs.apiType
                    )
                }
            }.onFailure {
                if (it is CancellationException) throw it
                logger.fWarn { "Send heartbeat failed: ${it.stackTraceToString()}" }
            }
        }
    }

    // ---------------- 工具 ----------------

    private fun checkLogin(action: ShortsAction): Boolean {
        if (isLoggedIn) return true
        _events.tryEmit(ShortsEvent.LoginRequired(action))
        return false
    }

    private fun emitFailure(action: ShortsAction, error: Throwable?) {
        val event = if (error is AuthFailureException) ShortsEvent.LoginRequired(action)
        else ShortsEvent.ActionFailed(action, error?.localizedMessage)
        _events.tryEmit(event)
    }

    private fun updateItemCounts(
        aid: Long,
        likeDelta: Int = 0,
        coinDelta: Int = 0,
        favoriteDelta: Int = 0
    ) {
        val index = items.indexOfFirst { it.aid == aid }
        if (index < 0) return
        val item = items[index]
        items[index] = item.copy(
            likeCount = item.likeCount?.plus(likeDelta)?.coerceAtLeast(0),
            coinCount = item.coinCount?.plus(coinDelta)?.coerceAtLeast(0),
            favoriteCount = item.favoriteCount?.plus(favoriteDelta)?.coerceAtLeast(0)
        )
    }
}

/**
 * 单个视频的操作状态
 *
 * @param coinCount 已投硬币数（0、1、2）
 * @param inToView 本次会话中是否已加入稍后再看
 * @param loaded 是否已从服务端取到点赞/投币/收藏状态
 */
@Immutable
data class ShortsActionState(
    val liked: Boolean = false,
    val coinCount: Int = 0,
    val favoured: Boolean = false,
    val inToView: Boolean = false,
    val loaded: Boolean = false
)

enum class ShortsAction {
    Like, Unlike, Coin, Favorite, Follow, Unfollow, AddToView, DelToView
}

sealed interface ShortsEvent {
    /** [id] 为 aid（关注类操作为 UP 主 mid） */
    data class ActionSucceeded(val action: ShortsAction, val id: Long) : ShortsEvent
    data class ActionFailed(val action: ShortsAction, val message: String?) : ShortsEvent
    data class LoginRequired(val action: ShortsAction) : ShortsEvent
    data class LoadFailed(val message: String?) : ShortsEvent
}
