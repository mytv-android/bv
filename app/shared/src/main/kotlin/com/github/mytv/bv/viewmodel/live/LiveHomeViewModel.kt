package com.github.mytv.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.entity.live.FollowLiveRoom
import dev.aaa1115910.biliapi.http.entity.live.LiveAreaParent
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.repositories.LiveRepository
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.util.fError
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.fWarn
import com.github.mytv.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

/**
 * 直播首页数据：推荐（一次加载，接口无分页参数）、关注（登录才加载，失败静默置空）、
 * 分区（两级分区列表 + 分区房间分页）。切 tab / 切分区由 UI 调对应 ensure 方法。
 */
@KoinViewModel
class LiveHomeViewModel(
    private val liveRepository: LiveRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    // ---------- 推荐 ----------

    val recommendList = mutableStateListOf<LiveRoomItem>()
    var recommendLoading by mutableStateOf(false)
        private set
    var recommendLoaded by mutableStateOf(false)
        private set

    fun ensureRecommend() {
        if (!recommendLoaded && !recommendLoading) loadRecommend()
    }

    fun refreshRecommend() {
        if (!recommendLoading) loadRecommend()
    }

    private fun loadRecommend() {
        recommendLoading = true
        viewModelScope.launch {
            try {
                logger.fInfo { "Load live recommend" }
                val data = liveRepository.getRecommend()
                withContext(Dispatchers.Main) {
                    recommendList.clear()
                    recommendList.addAll(data.list)
                }
                recommendLoaded = true
            } catch (e: Exception) {
                logger.fError { "Load live recommend failed: ${e.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    "加载直播推荐失败: ${e.localizedMessage}".toast(BVApp.context)
                }
            } finally {
                recommendLoading = false
            }
        }
    }

    // ---------- 关注 ----------

    val followingList = mutableStateListOf<LiveRoomItem>()
    var followingLoading by mutableStateOf(false)
        private set
    var followingLoaded by mutableStateOf(false)
        private set

    /** 登录才加载；未登录直接跳过（UI 显示登录提示）。失败静默置空（保留空列表，可重试）。 */
    fun ensureFollowing(login: Boolean) {
        if (!login || followingLoaded || followingLoading) return
        loadFollowing()
    }

    fun refreshFollowing(login: Boolean) {
        if (!login || followingLoading) return
        loadFollowing()
    }

    private fun loadFollowing() {
        followingLoading = true
        viewModelScope.launch {
            try {
                logger.fInfo { "Load follow live" }
                val data = liveRepository.getFollowing()
                val rooms = (data.rooms.takeIf { it.isNotEmpty() } ?: data.list).map { it.toItem() }
                withContext(Dispatchers.Main) {
                    followingList.clear()
                    followingList.addAll(rooms)
                }
                followingLoaded = true
            } catch (e: Exception) {
                // 登录失效等原因都静默处理：列表保持为空，未登录由 UI 提示
                logger.fWarn { "Load follow live failed: ${e.stackTraceToString()}" }
            } finally {
                followingLoading = false
            }
        }
    }

    private fun FollowLiveRoom.toItem() = LiveRoomItem(
        roomId = roomId.toInt(),
        uid = uid,
        title = title.ifBlank { roomname },
        uname = uname.ifBlank { nickname },
        online = online,
        cover = coverFromUser,
        userCover = keyframe,
        face = face,
        areaV2Id = areaV2Id,
        areaV2Name = areaV2Name,
        areaV2ParentId = areaV2ParentId,
        areaV2ParentName = areaV2ParentName,
        liveStatus = liveStatus,
    )

    // ---------- 分区 ----------

    val areaParents = mutableStateListOf<LiveAreaParent>()
    var areaListLoading by mutableStateOf(false)
        private set

    val areaRooms = mutableStateListOf<LiveRoomItem>()
    var areaRoomsLoading by mutableStateOf(false)
        private set
    var areaHasMore by mutableStateOf(false)
        private set

    /** 选中的一级分区下标；[selectedChild] 0 = 全部子分区，1..n = 子分区下标 + 1。 */
    var selectedParent by mutableIntStateOf(0)
        private set
    var selectedChild by mutableIntStateOf(0)
        private set
    private var areaPage = 0

    /** 防止旧的分区房间请求覆盖新选择的结果。 */
    private var areaLoadGeneration = 0

    fun ensureAreaList() {
        if (areaParents.isEmpty() && !areaListLoading) loadAreaList()
    }

    private fun loadAreaList() {
        areaListLoading = true
        viewModelScope.launch {
            try {
                logger.fInfo { "Load live area list" }
                val list = liveRepository.getAreaList()
                withContext(Dispatchers.Main) {
                    areaParents.clear()
                    areaParents.addAll(list)
                }
                if (list.isNotEmpty()) selectArea(0, 0)
            } catch (e: Exception) {
                logger.fError { "Load live area list failed: ${e.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    "加载直播分区失败: ${e.localizedMessage}".toast(BVApp.context)
                }
            } finally {
                areaListLoading = false
            }
        }
    }

    /**
     * 切换分区：重置并加载第一页。
     * [parentIndex] 一级分区下标，[childIndex] 0 = 全部子分区。
     */
    fun selectArea(parentIndex: Int, childIndex: Int) {
        val parent = areaParents.getOrNull(parentIndex) ?: return
        selectedParent = parentIndex
        selectedChild = childIndex.coerceIn(0, parent.list.size)
        loadAreaRooms(reset = true)
    }

    fun loadMoreAreaRooms() {
        loadAreaRooms(reset = false)
    }

    private fun loadAreaRooms(reset: Boolean) {
        if (!reset && (areaRoomsLoading || !areaHasMore)) return
        val parent = areaParents.getOrNull(selectedParent) ?: return
        val child = parent.list.getOrNull(selectedChild - 1)
        val page = if (reset) 1 else areaPage + 1
        val generation = ++areaLoadGeneration
        areaRoomsLoading = true
        viewModelScope.launch {
            try {
                logger.fInfo { "Load area rooms [parent=${parent.id}, area=${child?.id ?: 0}, page=$page]" }
                val result = liveRepository.getAreaRooms(parent.id, child?.id ?: 0, page)
                if (generation != areaLoadGeneration) return@launch
                withContext(Dispatchers.Main) {
                    if (reset) areaRooms.clear()
                    areaRooms.addAll(result.list)
                }
                areaPage = page
                areaHasMore = result.hasMore
            } catch (e: Exception) {
                if (generation != areaLoadGeneration) return@launch
                logger.fWarn { "Load area rooms failed: ${e.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    "加载分区直播间失败: ${e.localizedMessage}".toast(BVApp.context)
                }
                if (reset) areaHasMore = false
            } finally {
                if (generation == areaLoadGeneration) areaRoomsLoading = false
            }
        }
    }
}
