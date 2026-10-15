package dev.aaa1115910.bv.ui.feature.pgc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemDrawInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.CarouselData
import dev.aaa1115910.biliapi.entity.pgc.PgcFeedData
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.season.Timeline
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.ui.feature.timeline.SeasonFollowActions
import dev.aaa1115910.bv.ui.feature.timeline.fetchTimelines
import dev.aaa1115910.bv.ui.feature.timeline.forWeekday
import dev.aaa1115910.bv.ui.feature.timeline.todayDayOfWeek
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.pgc.FeedListType
import dev.aaa1115910.bv.viewmodel.pgc.PgcAnimeViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcDocumentaryViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcGuoChuangViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcMovieViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcTvViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcVarietyViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 顶部标签（顺序固定）：番剧(默认) 国创 电影 电视剧 纪录片 综艺。 */
internal val PgcTabs: List<PgcType> = listOf(
    PgcType.Anime, PgcType.GuoChuang, PgcType.Movie, PgcType.Tv, PgcType.Documentary, PgcType.Variety,
)

@Composable
internal fun pgcTabNames(): List<String> = listOf(
    stringResource(R.string.pgc_tab_anime),
    stringResource(R.string.pgc_tab_guochuang),
    stringResource(R.string.pgc_tab_movie),
    stringResource(R.string.pgc_tab_tv),
    stringResource(R.string.pgc_tab_documentary),
    stringResource(R.string.pgc_tab_variety),
)

/** 新番时间表只有番剧和国创有；电影 / 电视剧 / 纪录片 / 综艺隐藏时间表区块。 */
internal val PgcType.timelineFilter: TimelineFilter?
    get() = when (this) {
        PgcType.Anime -> TimelineFilter.Anime
        PgcType.GuoChuang -> TimelineFilter.GuoChuang
        else -> null
    }

/**
 * 每个分类的 ViewModel 按需创建（创建时就会请求轮播与推荐，旧电视版 PgcContent 一次创建全部 6 个）。
 * 存在影视页导航项的 ViewModelStore 里，切换标签后仍保留。
 */
@Composable
internal fun pgcViewModel(type: PgcType): PgcViewModel = when (type) {
    PgcType.Anime -> koinViewModel<PgcAnimeViewModel>()
    PgcType.GuoChuang -> koinViewModel<PgcGuoChuangViewModel>()
    PgcType.Movie -> koinViewModel<PgcMovieViewModel>()
    PgcType.Tv -> koinViewModel<PgcTvViewModel>()
    PgcType.Documentary -> koinViewModel<PgcDocumentaryViewModel>()
    PgcType.Variety -> koinViewModel<PgcVarietyViewModel>()
}

/** 精选（轮播）条目的补充信息：轮播数据只有封面、标题和 id，其余按需请求番剧详情（与番剧详情页同一个接口）。 */
internal data class PgcSeasonExtra(
    /** 最新一话的序号（纯数字时），用于「更新至第 N 话」「看第 N 话」。 */
    val latestEpisode: String?,
    /** 「每周五 23:00」，取不到为 null。 */
    val schedule: String?,
    /** 已完结等描述（latestEpisode 为 null 或已完结时显示）。 */
    val statusText: String?,
    val styles: List<String>,
    val follow: Boolean,
    val seasonId: Int,
)

private val ScheduleRegex = Regex("每周.\\s*\\d{1,2}:\\d{2}")

private fun SeasonDetail.toExtra(): PgcSeasonExtra {
    val finished = newEpDesc.contains("完结")
    val latest = episodes.lastOrNull()?.title?.trim()?.takeIf { it.toIntOrNull() != null }
    return PgcSeasonExtra(
        latestEpisode = latest,
        schedule = ScheduleRegex.find(newEpDesc)?.value,
        statusText = newEpDesc.takeIf { it.isNotBlank() && (finished || latest == null) },
        styles = styles,
        follow = userStatus.follow,
        seasonId = seasonId,
    )
}

/** 轮播条目的 key：有 seasonId 用 seasonId，否则用 -epId。 */
private fun CarouselData.CarouselItem.extraKey(): Int? {
    val sid = seasonId?.takeIf { it > 0 }
    val eid = episodeId?.takeIf { it > 0 }
    return sid ?: eid?.let { -it }
}

/**
 * 影视页自己的状态（不放进现有 ViewModel）：各分类的时间表、精选补充信息、当前选中的星期。
 * 网络请求在 IO 线程，状态只在主线程写。
 */
internal class PgcPageModel(
    private val seasonRepository: SeasonRepository,
    private val videoDetailRepository: VideoDetailRepository,
) : ViewModel() {
    private val timelines = mutableStateMapOf<TimelineFilter, List<Timeline>>()
    private val timelineLoading = mutableStateMapOf<TimelineFilter, Boolean>()
    private val timelineRequested = mutableSetOf<TimelineFilter>()
    private val extras = mutableStateMapOf<Int, PgcSeasonExtra>()
    private val extraRequested = mutableSetOf<Int>()

    /** 手机时间表选中的星期（1 = 周一 … 7 = 周日），默认今天。 */
    var selectedDay by mutableIntStateOf(todayDayOfWeek())

    /** 各分类当前精选的下标。 */
    private val featuredIndexes = mutableStateMapOf<PgcType, Int>()

    fun featuredIndex(type: PgcType): Int = featuredIndexes[type] ?: 0
    fun setFeaturedIndex(type: PgcType, index: Int) {
        featuredIndexes[type] = index
    }

    fun timelinesOf(filter: TimelineFilter): List<Timeline> = timelines[filter].orEmpty()
    fun isTimelineLoading(filter: TimelineFilter): Boolean = timelineLoading[filter] == true

    fun ensureTimeline(filter: TimelineFilter) {
        if (!timelineRequested.add(filter)) return
        timelineLoading[filter] = true
        viewModelScope.launch {
            runCatching { fetchTimelines(seasonRepository, filter) }
                .onSuccess { timelines[filter] = it }
                .onFailure { timelineRequested.remove(filter) }
            timelineLoading[filter] = false
        }
    }

    fun reloadTimeline(filter: TimelineFilter) {
        timelineRequested.remove(filter)
        ensureTimeline(filter)
    }

    fun extraOf(item: CarouselData.CarouselItem): PgcSeasonExtra? = item.extraKey()?.let { extras[it] }

    fun ensureExtra(item: CarouselData.CarouselItem) {
        val key = item.extraKey() ?: return
        if (!extraRequested.add(key)) return
        viewModelScope.launch {
            val detail = runCatching {
                withContext(Dispatchers.IO) {
                    videoDetailRepository.getPgcVideoDetail(
                        epid = if (key < 0) -key else null,
                        seasonId = if (key > 0) key else null,
                        preferApiType = Prefs.apiType,
                    )
                }
            }.getOrNull()
            if (detail != null) extras[key] = detail.toExtra()
        }
    }
}

/** 一个分类的界面数据：精选（轮播）、「热门」（推荐里的剧集合并为一行）、排行榜行。 */
@Stable
internal class PgcFeed(val vm: PgcViewModel) {
    val featured: List<CarouselData.CarouselItem> get() = vm.carouselItems

    val hot: List<PgcItem> by derivedStateOf {
        vm.feedItems.filter { it.type == FeedListType.Ep }.flatMap { it.items.orEmpty() }
    }

    val ranks: List<PgcFeedData.FeedRank> by derivedStateOf {
        vm.feedItems.filter { it.type == FeedListType.Rank }.mapNotNull { it.rank }
    }

    /** 推荐数据里的评分（轮播数据没有评分）。 */
    fun ratingOf(seasonId: Int?): String? {
        if (seasonId == null || seasonId <= 0) return null
        return (hot.asSequence() + ranks.asSequence().flatMap { it.items.asSequence() })
            .firstOrNull { it.seasonId == seasonId }
            ?.rating
            ?.takeIf { it.isNotBlank() }
    }

    fun loadMore() {
        if (!vm.updating && vm.hasNext) vm.loadMore()
    }
}

@Composable
internal fun rememberPgcFeed(vm: PgcViewModel): PgcFeed = remember(vm) { PgcFeed(vm) }

internal fun PgcItem.toCard(): SeasonCardData = SeasonCardData.fromPgcItem(this)

/** 打开番剧详情：seasonId / epId 至少一个有效。 */
internal fun NavController.openSeason(seasonId: Int?, epId: Int? = null) {
    val sid = seasonId?.takeIf { it > 0 }
    val eid = epId?.takeIf { it > 0 }
    if (sid == null && eid == null) return
    navigate(SeasonRoute(seasonId = sid, epId = eid))
}

internal fun NavController.openFeatured(item: CarouselData.CarouselItem) = openSeason(item.seasonId, item.episodeId)

/** 「更新至第 N 话 · 每周五 23:00」（取不到的部分省略）。 */
@Composable
internal fun PgcSeasonExtra.progressLine(withStyles: Boolean = false): String {
    val parts = mutableListOf<String>()
    if (statusText != null) parts += statusText
    else if (latestEpisode != null) parts += stringResource(R.string.pgc_updated_to, latestEpisode)
    if (schedule != null && statusText == null) parts += schedule
    if (withStyles && styles.isNotEmpty()) parts += styles.take(3).joinToString(" / ")
    return parts.joinToString(" · ")
}

/** 「看第 N 话」，没有话数时「立即观看」。 */
@Composable
internal fun watchLabel(extra: PgcSeasonExtra?): String =
    extra?.latestEpisode?.let { stringResource(R.string.pgc_watch_episode, it) }
        ?: stringResource(R.string.pgc_watch)

/** 精选条目当前是否已追番（本次运行中的操作优先，其次是详情接口的 userStatus）。 */
internal fun SeasonFollowActions.isFollowed(item: CarouselData.CarouselItem, extra: PgcSeasonExtra?): Boolean {
    val sid = extra?.seasonId ?: item.seasonId ?: return false
    return isFollowed(sid, extra?.follow ?: false)
}

internal fun SeasonFollowActions.toggle(item: CarouselData.CarouselItem, extra: PgcSeasonExtra?) {
    val sid = extra?.seasonId ?: item.seasonId?.takeIf { it > 0 } ?: return
    toggle(sid, extra?.follow ?: false)
}

/** 当周第 [day] 天的更新列表。 */
internal fun List<Timeline>.episodesOf(day: Int) = forWeekday(day)?.episodes.orEmpty()

/** 小项（被压缩的轮播项）不显示文字层：按当前宽度在大项尺寸的 60%→100% 之间淡入（同首页手机轮播）。 */
internal fun CarouselItemDrawInfo.textAlpha(): Float {
    val range = maxSize - minSize
    if (range <= 0f) return 1f
    val fraction = ((size - minSize) / range).coerceIn(0f, 1f)
    return ((fraction - 0.6f) / 0.4f).coerceIn(0f, 1f)
}

/** 时钟「HH:mm」15sp SemiBold onSurfaceVariant，每分钟刷新（原型 tvTop 右侧）。 */
@Composable
internal fun PgcTvClock(modifier: Modifier = Modifier) {
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var text by remember { mutableStateOf(format.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            text = format.format(Date())
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    Text(
        text,
        modifier = modifier,
        style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 精选还没加载出来时的占位块（圆角 + surfaceContainerHigh）。 */
@Composable
internal fun PgcHeroPlaceholder(modifier: Modifier, corner: Int = 28) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(corner.dp))
    )
}
