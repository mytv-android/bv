package com.github.mytv.bv.viewmodel.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.VideoActionRepository
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
 * 视频详情页的操作：点赞、投币、收藏（收藏夹多选）、稍后再看、关注 UP 主，以及 UP 主粉丝数。
 *
 * - 全部走 [VideoActionRepository]（与刷刷相同）；
 * - 收藏逻辑照抄旧电视版 FavoriteButton：未收藏时直接加入默认收藏夹，已收藏时由界面弹出收藏夹多选对话框，
 *   每次勾选立即提交（[updateFavoriteFolders]）；
 * - 所有 Compose 状态只在主线程（viewModelScope 默认的 Main.immediate）写入，网络请求在 IO 线程；
 * - 结果通过 [events] 通知界面（提示文案由界面决定）。
 *
 * 计数（点赞数、投币数、收藏数）由界面在视频详情的统计数上加 [likeDelta] / [coinDelta] / [favoriteDelta] 显示。
 */
@KoinViewModel
class VideoActionsViewModel(
    private val videoActionRepository: VideoActionRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }

        /** 旧电视版用标题识别默认收藏夹，找不到时用第一个收藏夹 */
        private const val DEFAULT_FOLDER_TITLE = "默认收藏夹"
    }

    /** 当前操作的视频 */
    var aid by mutableLongStateOf(0L)
        private set

    /** 当前视频的 UP 主（番剧为 0） */
    var upMid by mutableLongStateOf(0L)
        private set

    var liked by mutableStateOf(false)
        private set

    /** 已投硬币数（0、1、2） */
    var coinCount by mutableIntStateOf(0)
        private set

    var favoured by mutableStateOf(false)
        private set

    /** 本次会话中是否已加入稍后再看（服务端没有便宜的查询接口） */
    var inToView by mutableStateOf(false)
        private set

    /** 是否已关注 UP 主，null 表示未知（未登录或查询失败） */
    var following by mutableStateOf<Boolean?>(null)
        private set

    /** UP 主粉丝数，null 表示未知 */
    var followerCount by mutableStateOf<Int?>(null)
        private set

    var likeDelta by mutableIntStateOf(0)
        private set
    var coinDelta by mutableIntStateOf(0)
        private set
    var favoriteDelta by mutableIntStateOf(0)
        private set

    /** 收藏夹列表（收藏夹多选对话框用），属于视频 [aid] */
    val favoriteFolders: SnapshotStateList<FavoriteFolderMetadata> = mutableStateListOf()
    var favoriteFoldersLoading by mutableStateOf(false)
        private set
    private var favoriteFoldersAid = 0L

    private val _events = MutableSharedFlow<VideoActionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<VideoActionEvent> = _events.asSharedFlow()

    val isLoggedIn: Boolean get() = videoActionRepository.isLoggedIn

    private var loadJob: Job? = null
    private val pendingActions = mutableSetOf<String>()

    /**
     * 切换到视频[aid]（UP 主[upMid]，番剧传 null）：重置状态并查询点赞 / 投币 / 收藏 / 关注状态与粉丝数。
     * 同一个视频重复调用不会重新查询（除非[force]）。
     *
     * @param initialLiked / initialFavoured / initialCoined 视频详情接口里已带的用户状态（App 接口有），先显示
     */
    fun load(
        aid: Long,
        upMid: Long? = null,
        initialLiked: Boolean = false,
        initialFavoured: Boolean = false,
        initialCoined: Boolean = false,
        force: Boolean = false
    ) {
        if (!force && this.aid == aid && aid != 0L) return
        logger.fInfo { "Load video action state: [aid=$aid, upMid=$upMid]" }
        loadJob?.cancel()
        this.aid = aid
        this.upMid = upMid ?: 0L
        liked = initialLiked
        coinCount = if (initialCoined) 1 else 0
        favoured = initialFavoured
        inToView = false
        following = null
        followerCount = null
        likeDelta = 0
        coinDelta = 0
        favoriteDelta = 0
        favoriteFolders.clear()
        favoriteFoldersAid = 0L

        loadJob = viewModelScope.launch {
            val mid = upMid?.takeIf { it > 0 }
            if (mid != null) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        BiliHttpApi.getRelationStat(
                            mid = mid,
                            sessData = Prefs.sessData.takeIf { it.isNotBlank() }
                        ).getResponseData().follower
                    }
                }.onSuccess {
                    if (this@VideoActionsViewModel.aid == aid) followerCount = it
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Load follower count of $mid failed: ${it.localizedMessage}" }
                }
            }
            if (!isLoggedIn) return@launch
            runCatching {
                withContext(Dispatchers.IO) {
                    videoActionRepository.getActionState(
                        aid = aid,
                        upMid = mid,
                        preferApiType = Prefs.apiType
                    )
                }
            }.onSuccess { state ->
                if (this@VideoActionsViewModel.aid != aid) return@onSuccess
                liked = state.liked || liked
                coinCount = maxOf(state.coinCount, coinCount)
                favoured = state.favoured
                following = state.following
            }.onFailure {
                if (it is CancellationException) throw it
                logger.fWarn { "Load action state of av$aid failed: ${it.stackTraceToString()}" }
            }
        }
    }

    // ---------------- 点赞 ----------------

    fun toggleLike() {
        val aid = aid.takeIf { it != 0L } ?: return
        val like = !liked
        val action = if (like) VideoAction.Like else VideoAction.Unlike
        if (!checkLogin(action)) return
        if (!pendingActions.add("like")) return
        // 先更新界面（按钮形变需要立即响应），失败时回滚
        liked = like
        likeDelta += if (like) 1 else -1
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) { videoActionRepository.likeVideo(aid, like) }
                }.onSuccess {
                    _events.tryEmit(VideoActionEvent.Succeeded(action))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "${action.name} av$aid failed: ${it.stackTraceToString()}" }
                    if (this@VideoActionsViewModel.aid == aid) {
                        liked = !like
                        likeDelta -= if (like) 1 else -1
                    }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("like")
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
    fun coin(multiply: Int, alsoLike: Boolean) {
        val aid = aid.takeIf { it != 0L } ?: return
        val action = VideoAction.Coin
        if (!checkLogin(action)) return
        if (multiply !in 1..2) return
        if (!pendingActions.add("coin")) return
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        videoActionRepository.coinVideo(aid, multiply, alsoLike)
                    }
                }.onSuccess {
                    if (this@VideoActionsViewModel.aid != aid) return@onSuccess
                    coinCount = (coinCount + multiply).coerceAtMost(2)
                    coinDelta += multiply
                    if (alsoLike && !liked) {
                        liked = true
                        likeDelta += 1
                    }
                    _events.tryEmit(VideoActionEvent.Succeeded(action))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Coin av$aid failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("coin")
            }
        }
    }

    // ---------------- 收藏 ----------------

    /** 加载收藏夹列表（收藏夹多选对话框用），结果在 [favoriteFolders] */
    fun loadFavoriteFolders() {
        val aid = aid.takeIf { it != 0L } ?: return
        if (!checkLogin(VideoAction.Favorite)) return
        if (favoriteFoldersLoading) return
        favoriteFoldersLoading = true
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        videoActionRepository.getFavoriteFolders(aid, Prefs.apiType)
                    }
                }.onSuccess { folders ->
                    if (this@VideoActionsViewModel.aid != aid) return@onSuccess
                    favoriteFoldersAid = aid
                    favoriteFolders.swapList(folders)
                    favoured = folders.any { it.videoInThisFav }
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Load favorite folders failed: ${it.stackTraceToString()}" }
                    emitFailure(VideoAction.Favorite, it)
                }
            } finally {
                favoriteFoldersLoading = false
            }
        }
    }

    /** 未收藏时：加入默认收藏夹（旧电视版 addVideoToDefaultFavoriteFolder） */
    fun addToDefaultFavoriteFolder() {
        val aid = aid.takeIf { it != 0L } ?: return
        val action = VideoAction.Favorite
        if (!checkLogin(action)) return
        if (!pendingActions.add("fav")) return
        viewModelScope.launch {
            try {
                runCatching {
                    val folders = if (favoriteFoldersAid == aid && favoriteFolders.isNotEmpty()) {
                        favoriteFolders.toList()
                    } else {
                        withContext(Dispatchers.IO) {
                            videoActionRepository.getFavoriteFolders(aid, Prefs.apiType)
                        }
                    }
                    val defaultFolder = folders.firstOrNull { it.title == DEFAULT_FOLDER_TITLE }
                        ?: folders.firstOrNull()
                    requireNotNull(defaultFolder) { "Not found default favorite folder" }
                    val selected = (folders.filter { it.videoInThisFav }.map { it.id } + defaultFolder.id).distinct()
                    withContext(Dispatchers.IO) {
                        videoActionRepository.updateFavoriteFolders(
                            aid = aid,
                            selectedFolderIds = selected,
                            allFolderIds = folders.map { it.id },
                            preferApiType = Prefs.apiType
                        )
                    }
                    folders to selected
                }.onSuccess { (folders, selected) ->
                    if (this@VideoActionsViewModel.aid != aid) return@onSuccess
                    favoriteFoldersAid = aid
                    favoriteFolders.swapList(folders.map { it.copy(videoInThisFav = it.id in selected) })
                    if (!favoured) favoriteDelta += 1
                    favoured = true
                    _events.tryEmit(VideoActionEvent.Succeeded(action))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "Add av$aid to default favorite folder failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("fav")
            }
        }
    }

    /**
     * 把视频放进[selectedFolderIds]这些收藏夹，并从 [favoriteFolders] 里其余的收藏夹移除
     * （需先 [loadFavoriteFolders]）
     */
    fun updateFavoriteFolders(selectedFolderIds: List<Long>) {
        val aid = aid.takeIf { it != 0L } ?: return
        val action = VideoAction.Favorite
        if (!checkLogin(action)) return
        if (favoriteFoldersAid != aid || favoriteFolders.isEmpty()) {
            logger.fWarn { "Favorite folders of av$aid are not loaded" }
            return
        }
        val allFolderIds = favoriteFolders.map { it.id }
        val before = favoriteFolders.toList()
        val wasFavoured = favoured
        // 先更新界面，失败时回滚
        favoriteFolders.swapList(before.map { it.copy(videoInThisFav = it.id in selectedFolderIds) })
        val nowFavoured = selectedFolderIds.isNotEmpty()
        favoured = nowFavoured
        if (nowFavoured != wasFavoured) favoriteDelta += if (nowFavoured) 1 else -1
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    videoActionRepository.updateFavoriteFolders(
                        aid = aid,
                        selectedFolderIds = selectedFolderIds,
                        allFolderIds = allFolderIds,
                        preferApiType = Prefs.apiType
                    )
                }
            }.onFailure {
                if (it is CancellationException) throw it
                logger.fWarn { "Update favorite folders of av$aid failed: ${it.stackTraceToString()}" }
                if (this@VideoActionsViewModel.aid == aid) {
                    favoriteFolders.swapList(before)
                    if (nowFavoured != wasFavoured) favoriteDelta -= if (nowFavoured) 1 else -1
                    favoured = wasFavoured
                }
                emitFailure(action, it)
            }
        }
    }

    // ---------------- 关注 ----------------

    /** 关注 / 取消关注 UP 主（照抄旧电视版 UpButton：操作后重新查询关注状态） */
    fun toggleFollow() {
        val mid = upMid.takeIf { it > 0 } ?: return
        val isFollowing = following == true
        val action = if (isFollowing) VideoAction.Unfollow else VideoAction.Follow
        if (!checkLogin(action)) return
        if (!pendingActions.add("follow")) return
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        if (isFollowing) videoActionRepository.unfollowUser(mid, Prefs.apiType)
                        else videoActionRepository.followUser(mid, Prefs.apiType)
                    }
                }.onSuccess { success ->
                    if (!success) {
                        emitFailure(action, null)
                        return@onSuccess
                    }
                    val checked = runCatching {
                        withContext(Dispatchers.IO) { videoActionRepository.checkFollowing(mid, Prefs.apiType) }
                    }.getOrNull()
                    if (upMid == mid) {
                        following = checked ?: !isFollowing
                        followerCount = followerCount?.plus(if (isFollowing) -1 else 1)?.coerceAtLeast(0)
                    }
                    _events.tryEmit(VideoActionEvent.Succeeded(action))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "${action.name} $mid failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("follow")
            }
        }
    }

    // ---------------- 稍后再看 ----------------

    fun toggleToView() {
        val aid = aid.takeIf { it != 0L } ?: return
        val remove = inToView
        val action = if (remove) VideoAction.DelToView else VideoAction.AddToView
        if (!checkLogin(action)) return
        if (!pendingActions.add("toview")) return
        viewModelScope.launch {
            try {
                runCatching {
                    withContext(Dispatchers.IO) {
                        if (remove) videoActionRepository.delToView(aid)
                        else videoActionRepository.addToView(aid)
                    }
                }.onSuccess {
                    if (this@VideoActionsViewModel.aid == aid) inToView = !remove
                    _events.tryEmit(VideoActionEvent.Succeeded(action))
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.fWarn { "${action.name} av$aid failed: ${it.stackTraceToString()}" }
                    emitFailure(action, it)
                }
            } finally {
                pendingActions.remove("toview")
            }
        }
    }

    // ---------------- 工具 ----------------

    private fun checkLogin(action: VideoAction): Boolean {
        if (isLoggedIn) return true
        _events.tryEmit(VideoActionEvent.LoginRequired(action))
        return false
    }

    private fun emitFailure(action: VideoAction, error: Throwable?) {
        val event = if (error is AuthFailureException) VideoActionEvent.LoginRequired(action)
        else VideoActionEvent.Failed(action, error?.localizedMessage)
        _events.tryEmit(event)
    }
}

enum class VideoAction {
    Like, Unlike, Coin, Favorite, Follow, Unfollow, AddToView, DelToView
}

sealed interface VideoActionEvent {
    data class Succeeded(val action: VideoAction) : VideoActionEvent
    data class Failed(val action: VideoAction, val message: String?) : VideoActionEvent
    data class LoginRequired(val action: VideoAction) : VideoActionEvent
}
