package com.github.mytv.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.repositories.LivePlayInfo
import dev.aaa1115910.biliapi.http.entity.live.RoomInfoData
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocket
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.util.fError
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

/**
 * 直播间数据：房间信息 + 流播放信息（画质 / 线路）+ 弹幕 WebSocket 事件流。
 *
 * [load] 取房间信息和默认画质第一条线路；[switchLine] / [switchQuality] 重新请求流地址后
 * 选对应线路 / 画质（加载中忽略新请求）。弹幕事件保留最近 200 条（[danmakuEvents]），
 * 离开页面时随 viewModelScope 取消收集并断开 WebSocket。
 */
@KoinViewModel
class LivePlayerViewModel(
    private val liveRepository: LiveRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
        private const val DanmakuMaxSize = 200
    }

    var roomInfo by mutableStateOf<RoomInfoData?>(null)
        private set
    var playInfo by mutableStateOf<LivePlayInfo?>(null)
        private set

    /** 当前选中的线路下标（playInfo.lines 中的位置）。 */
    var currentLineIndex by mutableIntStateOf(0)
        private set

    /** 当前画质 qn（加载结果里的实际画质）。 */
    var currentQn by mutableIntStateOf(0)
        private set

    /** 加载 / 切换线路画质中（重入保护）。 */
    var loading by mutableStateOf(false)
        private set

    /** 正在直播且有可用线路。 */
    var playing by mutableStateOf(false)
        private set

    /** 加载失败信息；null = 无错误。 */
    var error by mutableStateOf<String?>(null)
        private set

    private val _danmakuEvents = MutableStateFlow<List<LiveEvent>>(emptyList())

    /** 弹幕 / 进房等实时事件，保留最近 200 条，主线程更新。 */
    val danmakuEvents: StateFlow<List<LiveEvent>> = _danmakuEvents.asStateFlow()

    private var roomId = 0
    private var danmakuJob: Job? = null

    /**
     * 加载直播间。同一直播间已加载成功时跳过（重进 / 重组合安全），失败后可重试。
     */
    fun load(roomId: Int) {
        if (loading) return
        if (this.roomId == roomId && roomInfo != null && error == null) return
        this.roomId = roomId
        loading = true
        playing = false
        error = null
        roomInfo = null
        playInfo = null
        currentLineIndex = 0
        currentQn = 0
        viewModelScope.launch {
            try {
                logger.fInfo { "Load live room [roomId=$roomId]" }
                val info = liveRepository.getRoomInfo(roomId)
                val play = liveRepository.getRoomPlayInfo(roomId)
                withContext(Dispatchers.Main) {
                    roomInfo = info
                    playInfo = play
                    currentQn = play.currentQn
                    currentLineIndex = 0
                    playing = info.liveStatus == 1 && play.lines.isNotEmpty()
                }
                startDanmaku(info.roomId.takeIf { it > 0 } ?: roomId)
            } catch (e: Exception) {
                logger.fError { "Load live room failed: ${e.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    error = e.localizedMessage ?: "加载直播间失败"
                    "加载直播间失败: ${e.localizedMessage}".toast(BVApp.context)
                }
            } finally {
                loading = false
            }
        }
    }

    /** 切换线路：重新请求流地址后选对应线路，画质不变。加载中忽略。 */
    fun switchLine(index: Int) {
        val info = playInfo ?: return
        if (loading || index !in info.lines.indices || index == currentLineIndex) return
        loading = true
        viewModelScope.launch {
            try {
                logger.fInfo { "Switch live line [roomId=$roomId, line=$index]" }
                val play = liveRepository.getRoomPlayInfo(roomId, currentQn)
                withContext(Dispatchers.Main) {
                    playInfo = play
                    currentLineIndex = index.coerceAtMost((play.lines.size - 1).coerceAtLeast(0))
                }
            } catch (e: Exception) {
                logger.fError { "Switch live line failed: ${e.stackTraceToString()}" }
                withContext(Dispatchers.Main) { "切换线路失败: ${e.localizedMessage}".toast(BVApp.context) }
            } finally {
                loading = false
            }
        }
    }

    /** 切换画质：重新请求流地址（保持当前线路下标），qn 以返回的 currentQn 为准。加载中忽略。 */
    fun switchQuality(qn: Int) {
        if (loading || qn == currentQn) return
        val lineIndex = currentLineIndex
        loading = true
        viewModelScope.launch {
            try {
                logger.fInfo { "Switch live quality [roomId=$roomId, qn=$qn]" }
                val play = liveRepository.getRoomPlayInfo(roomId, qn)
                withContext(Dispatchers.Main) {
                    playInfo = play
                    currentQn = play.currentQn
                    currentLineIndex = lineIndex.coerceAtMost((play.lines.size - 1).coerceAtLeast(0))
                }
            } catch (e: Exception) {
                logger.fError { "Switch live quality failed: ${e.stackTraceToString()}" }
                withContext(Dispatchers.Main) { "切换画质失败: ${e.localizedMessage}".toast(BVApp.context) }
            } finally {
                loading = false
            }
        }
    }

    /**
     * 重新请求当前画质 / 线路的流地址（刷新按钮、播放出错重试用）。加载中忽略。
     * 成功后 playInfo 整体替换，UI 以 url 变化为准重新起播。
     */
    fun refreshStream() {
        if (loading || playInfo == null) return
        val qn = currentQn
        val lineIndex = currentLineIndex
        loading = true
        viewModelScope.launch {
            try {
                logger.fInfo { "Refresh live stream [roomId=$roomId, qn=$qn, line=$lineIndex]" }
                val play = liveRepository.getRoomPlayInfo(roomId, qn)
                withContext(Dispatchers.Main) {
                    playInfo = play
                    currentQn = play.currentQn
                    currentLineIndex = lineIndex.coerceAtMost((play.lines.size - 1).coerceAtLeast(0))
                }
            } catch (e: Exception) {
                logger.fError { "Refresh live stream failed: ${e.stackTraceToString()}" }
                withContext(Dispatchers.Main) { "刷新播放失败: ${e.localizedMessage}".toast(BVApp.context) }
            } finally {
                loading = false
            }
        }
    }

    /** 连接弹幕 WebSocket，收集进 StateFlow；重新加载时先断开旧连接。 */
    private fun startDanmaku(realRoomId: Int) {
        danmakuJob?.cancel()
        danmakuJob = viewModelScope.launch {
            _danmakuEvents.value = emptyList()
            logger.fInfo { "Connect live danmaku [roomId=$realRoomId]" }
            LiveDataWebSocket.connectLiveEvent(realRoomId).collect { event ->
                // viewModelScope 在主线程，这里即主线程更新
                _danmakuEvents.update { events -> (events + event).takeLast(DanmakuMaxSize) }
            }
        }
    }

    override fun onCleared() {
        // 取消收集即断开 WebSocket（调用方通过取消 Flow 断开连接）
        danmakuJob?.cancel()
        super.onCleared()
    }
}
