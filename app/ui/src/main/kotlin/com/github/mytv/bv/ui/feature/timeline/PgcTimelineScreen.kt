package com.github.mytv.bv.ui.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.season.Timeline
import dev.aaa1115910.biliapi.entity.season.TimelineEp
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.BvChip
import com.github.mytv.bv.ui.designsystem.component.FullPageLoading
import com.github.mytv.bv.ui.designsystem.component.PageMessage
import com.github.mytv.bv.ui.designsystem.component.PosterCard
import com.github.mytv.bv.ui.designsystem.focus.TvColumnPivot
import com.github.mytv.bv.ui.navigation.SeasonRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** 新番时间表页的数据：一次请求（番剧，同旧 AnimeTimelineScreen），默认选中「今天」。 */
internal class TimelinePageModel(private val repository: SeasonRepository) : ViewModel() {
    var timelines by mutableStateOf<List<Timeline>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var failed by mutableStateOf(false)
        private set
    var selected by mutableIntStateOf(-1)
    private var started = false

    fun ensureLoaded() {
        if (started) return
        started = true
        load()
    }

    fun load() {
        loading = true
        failed = false
        viewModelScope.launch {
            val result = runCatching { fetchTimelines(repository, TimelineFilter.Anime) }
            result.onSuccess { list ->
                timelines = list
                if (selected !in list.indices) selected = list.indexOfFirst { it.isToday }.coerceAtLeast(0)
            }.onFailure {
                failed = true
            }
            loading = false
        }
    }
}

/** 新番时间表（第 9.11 节）：日期标签（今天预选）+ 当天更新的剧集。点击进入番剧详情。 */
@Composable
fun PgcTimelineScreen(nav: NavController) {
    val repository: SeasonRepository = koinInject()
    val model: TimelinePageModel = viewModel { TimelinePageModel(repository) }
    val follow = rememberSeasonFollow()
    LaunchedEffect(model) { model.ensureLoaded() }

    val title = stringResource(R.string.timeline_title)
    val onOpen: (TimelineEp) -> Unit = { nav.navigate(SeasonRoute(seasonId = it.seasonId)) }
    val onBack: () -> Unit = { nav.popBackStack() }
    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> TimelinePhone(title, model, follow, onOpen, onBack)
        DeviceLayout.Tablet -> TimelineTablet(title, model, onOpen, onBack)
        DeviceLayout.Tv -> TimelineTv(title, model, onOpen)
    }
}

/** 手机：LargeFlexibleTopAppBar + 日期标签 + 列表（时间 · 海报 · 标题 · 追番）。 */
@Composable
private fun TimelinePhone(
    title: String,
    model: TimelinePageModel,
    follow: SeasonFollowActions,
    onOpen: (TimelineEp) -> Unit,
    onBack: () -> Unit,
) {
    SubPagePhone(title = title, onBack = onBack) { bottom ->
        Column(Modifier.fillMaxSize()) {
            TimelineDayChips(model, contentPadding = PaddingValues(horizontal = 16.dp))
            TimelineDayContent(
                model,
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { day ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(day.episodes, key = { it.seasonId }, contentType = { "episode" }) { episode ->
                        TimelineEpisodeRow(
                            episode = episode,
                            followed = follow.isFollowed(episode.seasonId),
                            onClick = { onOpen(episode) },
                            onFollow = { follow.toggle(episode.seasonId) },
                        )
                    }
                }
            }
        }
    }
}

/** 平板：返回 + 标题、日期标签、Adaptive(120) 海报网格。 */
@Composable
private fun TimelineTablet(
    title: String,
    model: TimelinePageModel,
    onOpen: (TimelineEp) -> Unit,
    onBack: () -> Unit,
) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SubPageTabletHeader(title = title, onBack = onBack)
        TimelineDayChips(model, contentPadding = PaddingValues(horizontal = 24.dp))
        TimelineDayContent(
            model,
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { day ->
            val gridState = rememberLazyGridState()
            LaunchedEffect(model.selected) { gridState.scrollToItem(0) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = bottom),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(day.episodes, key = { _, episode -> episode.seasonId }, contentType = { _, _ -> "episode" }) { _, episode ->
                    TimelinePoster(episode, onClick = { onOpen(episode) })
                }
            }
        }
    }
}

/** 电视：标题 at (48, 27) + 日期标签（聚焦即选中）+ Fixed(6) 海报网格，第一项初始焦点。 */
@Composable
private fun TimelineTv(
    title: String,
    model: TimelinePageModel,
    onOpen: (TimelineEp) -> Unit,
) {
    val firstRequester = remember { FocusRequester() }
    val chipsRequester = remember { FocusRequester() }
    var entryPending by remember { mutableStateOf(true) }
    val day = model.timelines.getOrNull(model.selected)

    // 第一项初始焦点；没有内容时聚焦日期标签
    LaunchedEffect(model.loading, day?.episodes?.isNotEmpty()) {
        if (!entryPending || model.loading) return@LaunchedEffect
        withFrameNanos { }
        val focused = day?.episodes?.isNotEmpty() == true && firstRequester.tryRequestFocus()
        if (!focused) chipsRequester.tryRequestFocus()
        entryPending = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SubPageTvTitle(title) {
            TimelineDayChips(
                model,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                selectOnFocus = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(chipsRequester)
                    .focusRestorer()
                    .focusGroup(),
            )
        }
        TimelineDayContent(
            model,
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { shown ->
            val gridState = rememberLazyGridState()
            LaunchedEffect(model.selected) { gridState.scrollToItem(0) }
            TvColumnPivot {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRestorer()
                        .focusGroup(),
                    contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 27.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    itemsIndexed(shown.episodes, key = { _, episode -> episode.seasonId }, contentType = { _, _ -> "episode" }) { index, episode ->
                        TimelinePoster(
                            episode,
                            onClick = { onOpen(episode) },
                            modifier = if (index == 0) Modifier.focusRequester(firstRequester) else Modifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelinePoster(episode: TimelineEp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PosterCard(
        title = episode.title,
        cover = episode.thumbnail,
        onClick = onClick,
        modifier = modifier,
        subtitle = listOf(episode.publishTime, episode.publishIndex)
            .filter { it.isNotBlank() }
            .joinToString(" · "),
    )
}

/** 日期标签：「今天」或「周一 10-12」。 */
@Composable
private fun TimelineDayChips(
    model: TimelinePageModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    selectOnFocus: Boolean = false,
) {
    val weekdays = stringArrayResource(R.array.timeline_weekday)
    val today = stringResource(R.string.timeline_today)
    val listState = rememberLazyListState()
    LaunchedEffect(model.timelines) {
        if (model.selected > 0) listState.scrollToItem((model.selected - 1).coerceAtLeast(0))
    }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(model.timelines, key = { _, timeline -> timeline.dayOfWeek }, contentType = { _, _ -> "day" }) { index, timeline ->
            val label = if (timeline.isToday) today else stringResource(
                R.string.timeline_day_label,
                weekdays.getOrElse(timeline.dayOfWeek - 1) { "" },
                timeline.dateString,
            )
            BvChip(
                text = label,
                selected = index == model.selected,
                onClick = { model.selected = index },
                selectOnFocus = selectOnFocus,
            )
        }
    }
}

/** 加载中 / 失败（重试）/ 当天为空 / 内容。 */
@Composable
private fun TimelineDayContent(
    model: TimelinePageModel,
    modifier: Modifier,
    content: @Composable (Timeline) -> Unit,
) {
    val day = model.timelines.getOrNull(model.selected)
    Box(modifier) {
        when {
            model.timelines.isEmpty() && model.loading -> FullPageLoading()
            model.timelines.isEmpty() && model.failed -> PageMessage(
                message = stringResource(R.string.timeline_load_failed),
                onRetry = model::load,
            )

            day == null || day.episodes.isEmpty() -> PageMessage(message = stringResource(R.string.timeline_empty))
            else -> content(day)
        }
    }
}

