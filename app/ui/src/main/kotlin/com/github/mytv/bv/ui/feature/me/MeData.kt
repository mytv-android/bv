package com.github.mytv.bv.ui.feature.me

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonStatus
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.biliapi.entity.user.HistoryItem
import dev.aaa1115910.biliapi.entity.user.HistoryItemType
import dev.aaa1115910.biliapi.repositories.HistoryRepository
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.carddata.VideoCardData
import com.github.mytv.bv.ui.navigation.SeasonRoute
import com.github.mytv.bv.ui.navigation.openVideo
import com.github.mytv.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/** 一条历史记录：卡片（角标只显示总时长）+ 进度 + 观看时间 + 是否番剧。 */
internal data class HistoryEntry(
    val card: VideoCardData,
    val progress: Float,
    /** 观看时间，秒级时间戳；未知为 0 */
    val viewAt: Long,
    val pgc: Boolean,
    val seasonId: Int?,
    val epId: Int?,
)

/** 平板右栏历史记录的筛选标签：全部 / 视频 / 番剧。 */
internal enum class HistoryFilter(@param:StringRes val label: Int) {
    All(R.string.history_filter_all),
    Video(R.string.history_filter_video),
    Pgc(R.string.history_filter_pgc);

    fun accepts(entry: HistoryEntry): Boolean = when (this) {
        All -> true
        Video -> !entry.pgc
        Pgc -> entry.pgc
    }
}

/** 按观看日期分组：今天 / 昨天 / 更早。 */
internal enum class HistoryDay(@param:StringRes val label: Int) {
    Today(R.string.history_today),
    Yesterday(R.string.history_yesterday),
    Earlier(R.string.history_earlier),
}

/** 今天 0 点的秒级时间戳。 */
internal fun startOfTodaySeconds(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis / 1000

internal fun HistoryEntry.day(todayStart: Long): HistoryDay = when {
    viewAt <= 0L -> HistoryDay.Earlier
    viewAt >= todayStart -> HistoryDay.Today
    viewAt >= todayStart - 24 * 60 * 60 -> HistoryDay.Yesterday
    else -> HistoryDay.Earlier
}

/** 打开一条历史：番剧进番剧详情（有 epId 用 epId，否则 seasonId），其余进视频详情。 */
internal fun NavController.openHistory(entry: HistoryEntry) {
    val epId = entry.epId
    val seasonId = entry.seasonId
    when {
        entry.pgc && epId != null -> navigate(SeasonRoute(epId = epId))
        entry.pgc && seasonId != null -> navigate(SeasonRoute(seasonId = seasonId))
        else -> openVideo(entry.card)
    }
}

private fun HistoryItem.toEntry(): HistoryEntry {
    val finished = progress == -1
    val ratio = when {
        finished -> 1f
        duration > 0 -> (progress.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }
    val pgc = type == HistoryItemType.Pgc
    return HistoryEntry(
        card = VideoCardData(
            avid = oid,
            title = title,
            cover = cover,
            upName = author,
            time = duration * 1000L,
            upMid = authorMid.takeIf { it > 0 },
        ),
        progress = ratio,
        viewAt = viewAt,
        pgc = pgc,
        seasonId = seasonId?.takeIf { pgc && it > 0 },
        epId = epid?.takeIf { pgc && it > 0 },
    )
}

/**
 * 「我的」页的数据（第 9.8 节）：
 * - 历史记录：加载逻辑照抄 HistoryViewModel（HistoryRepository.getHistories + cursor 翻页），
 *   但保留观看时间与类型（今天看了 N 个、按日期分组、全部 / 视频 / 番剧），全部在主线程写状态；
 * - 追番追剧总数（番剧 + 影视，各取 1 条读 total）。
 */
internal class MeDataViewModel(
    private val historyRepository: HistoryRepository,
    private val seasonRepository: SeasonRepository,
) : ViewModel() {
    private val logger = KotlinLogging.logger("BvMeData")

    val entries = mutableStateListOf<HistoryEntry>()
    var loading by mutableStateOf(false)
        private set
    var noMore by mutableStateOf(false)
        private set
    var seasonTotal by mutableStateOf<Int?>(null)
        private set

    private var cursor = 0L

    /** 每次重置加一；旧请求回来时发现不一致就丢弃结果。 */
    private var generation = 0

    fun loadMore() {
        if (loading || noMore) return
        val token = generation
        loading = true
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    historyRepository.getHistories(cursor = cursor, preferApiType = Prefs.apiType)
                }
                if (token != generation) return@launch
                entries.addAll(data.data.map { it.toEntry() })
                cursor = data.cursor
                if (cursor == 0L) noMore = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.warn { "Load histories failed: ${e.message}" }
            } finally {
                if (token == generation) loading = false
            }
        }
    }

    /** 清空后从头加载（重试）。 */
    fun reload() {
        generation++
        loading = false
        cursor = 0L
        noMore = false
        entries.clear()
        loadMore()
    }

    /**
     * 回到页面时刷新：列表为空就从头加载；否则静默取第一页，最新一条变了才替换（保持滚动位置）。
     */
    fun refresh() {
        if (entries.isEmpty()) {
            if (!loading) reload()
            return
        }
        viewModelScope.launch {
            val data = runCatching {
                withContext(Dispatchers.IO) {
                    historyRepository.getHistories(cursor = 0L, preferApiType = Prefs.apiType)
                }
            }.getOrNull() ?: return@launch
            val fresh = data.data.map { it.toEntry() }
            val head = fresh.firstOrNull() ?: return@launch
            val current = entries.firstOrNull()
            if (current == null || current.card.avid != head.card.avid || current.viewAt != head.viewAt) {
                generation++
                loading = false
                entries.clear()
                entries.addAll(fresh)
                cursor = data.cursor
                noMore = cursor == 0L
            }
        }
    }

    fun loadSeasonTotal() {
        viewModelScope.launch {
            val total = runCatching {
                withContext(Dispatchers.IO) {
                    var sum = 0
                    for (type in FollowingSeasonType.entries) {
                        sum += seasonRepository.getFollowingSeasons(
                            type = type,
                            status = FollowingSeasonStatus.All,
                            pageNumber = 1,
                            pageSize = 1,
                            preferApiType = Prefs.apiType,
                        ).total
                    }
                    sum
                }
            }.getOrNull()
            if (total != null) seasonTotal = total
        }
    }
}
