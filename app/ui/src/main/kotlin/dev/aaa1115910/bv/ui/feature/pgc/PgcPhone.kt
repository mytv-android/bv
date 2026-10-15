package dev.aaa1115910.bv.ui.feature.pgc

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.CarouselData
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.CinemaScrim
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PosterCard
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.feature.timeline.TimelineEpisodeRow
import dev.aaa1115910.bv.ui.feature.timeline.todayDayOfWeek
import dev.aaa1115910.bv.ui.navigation.LocalBottomBarPadding
import dev.aaa1115910.bv.ui.navigation.PgcTimelineRoute
import dev.aaa1115910.bv.ui.navigation.SearchRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 手机影视（原型 pgcPhone）：「影视」+ 搜索、分类标签、竖海报轮播、新番时间表（一周按钮 + 当日更新）、
 * 「热门」行与排行榜行（PosterCard 112）。电影 / 电视剧 / 纪录片 / 综艺没有时间表区块。
 */
@Composable
internal fun PgcPhone(nav: NavController, state: PgcUiState) {
    val names = pgcTabNames()
    val feed = state.feed
    val vm = feed.vm
    val model = state.model
    val filter = state.type.timelineFilter
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var pulling by remember { mutableStateOf(false) }
    val bottom = LocalBottomBarPadding.current + 16.dp

    // 切换分类时回到顶部（从详情返回时不动）
    var shownTab by rememberSaveable { mutableIntStateOf(state.tab) }
    LaunchedEffect(state.tab) {
        if (shownTab != state.tab) {
            shownTab = state.tab
            listState.scrollToItem(0)
        }
    }
    listState.LoadMoreEffect { if (feed.hot.isNotEmpty()) feed.loadMore() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.pgc_title),
                style = BvType.PageTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BvIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.ds_search),
                onClick = { nav.navigate(SearchRoute()) },
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(names) { index, name ->
                BvChip(text = name, selected = index == state.tab, onClick = { state.onTab(index) })
            }
        }
        PullToRefreshBox(
            isRefreshing = pulling,
            onRefresh = {
                pulling = true
                vm.reloadAll()
                filter?.let(model::reloadTimeline)
                scope.launch {
                    delay(600)
                    withTimeoutOrNull(15_000L) { snapshotFlow { vm.updating }.first { !it } }
                    pulling = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (feed.featured.isEmpty() && feed.hot.isEmpty() && feed.ranks.isEmpty()) {
                EmptyOrLoading(
                    loading = vm.updating || pulling,
                    modifier = Modifier.fillMaxSize(),
                    message = stringResource(R.string.pgc_empty),
                    onRetry = { vm.reloadAll() },
                )
            } else {
                // 轮播数据晚于时间表到达时会插在可见项上方，LazyColumn 会保持锚点导致轮播被滚出屏幕：
                // 用户还没滚动时回到顶部
                val hasFeatured = feed.featured.isNotEmpty()
                LaunchedEffect(hasFeatured) {
                    if (hasFeatured && listState.firstVisibleItemIndex <= 1 && !listState.isScrollInProgress) {
                        listState.scrollToItem(0)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottom),
                ) {
                    if (feed.featured.isNotEmpty()) {
                        item(key = "carousel", contentType = "carousel") {
                            PhoneCarousel(
                                items = feed.featured,
                                state = state,
                                onOpen = { nav.openFeatured(it) },
                            )
                        }
                    }
                    if (filter != null) {
                        item(key = "timelineHeader", contentType = "header") {
                            SectionHeader(
                                title = stringResource(R.string.pgc_timeline),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                action = stringResource(R.string.pgc_timeline_full),
                                onAction = { nav.navigate(PgcTimelineRoute) },
                            )
                        }
                        item(key = "days", contentType = "days") {
                            PhoneDayButtons(selected = model.selectedDay, onSelect = { model.selectedDay = it })
                        }
                        val episodes = model.timelinesOf(filter).episodesOf(model.selectedDay)
                        if (episodes.isEmpty()) {
                            item(key = "dayEmpty", contentType = "dayEmpty") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (model.isTimelineLoading(filter)) {
                                        LoadingIndicator()
                                    } else {
                                        Text(
                                            stringResource(R.string.pgc_day_empty),
                                            style = BvType.Small,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(episodes, contentType = { _, _ -> "episode" }) { index, episode ->
                                TimelineEpisodeRow(
                                    episode = episode,
                                    followed = state.follow.isFollowed(episode.seasonId),
                                    onClick = { nav.openSeason(episode.seasonId) },
                                    onFollow = { state.follow.toggle(episode.seasonId) },
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = if (index == 0) 12.dp else 10.dp,
                                        bottom = if (index == episodes.lastIndex) 12.dp else 0.dp,
                                    ),
                                )
                            }
                        }
                    }
                    if (feed.hot.isNotEmpty()) {
                        item(key = "hot", contentType = "row") {
                            PhoneFeedRow(
                                title = stringResource(R.string.pgc_hot, names[state.tab]),
                                posters = feed.hot,
                                onOpen = { nav.openSeason(it.seasonId) },
                                onLoadMore = feed::loadMore,
                            )
                        }
                    }
                    itemsIndexed(feed.ranks, contentType = { _, _ -> "row" }) { _, rank ->
                        PhoneFeedRow(
                            title = rank.title,
                            posters = rank.items,
                            onOpen = { nav.openSeason(it.seasonId) },
                        )
                    }
                    if (vm.updating) {
                        item(key = "loading", contentType = "loading") { LoadMoreIndicator() }
                    }
                }
            }
        }
    }
}

/**
 * 竖海报轮播：padding(top 14, 水平 16) 高 290，HorizontalMultiBrowseCarousel(220, 8, 34, 90)。
 * 每项 3:4 Crop，maskClip 圆角 28；大项显示 CinemaScrim + 左下 (14,14) 标题 19sp + 「更新至第 N 话 · 每周五 23:00」。
 */
@Composable
private fun PhoneCarousel(
    items: List<CarouselData.CarouselItem>,
    state: PgcUiState,
    onOpen: (CarouselData.CarouselItem) -> Unit,
) {
    val count by rememberUpdatedState(items.size)
    val carouselState = rememberCarouselState { count }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 220.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp)
            .height(290.dp),
        itemSpacing = 8.dp,
        minSmallItemWidth = 34.dp,
        maxSmallItemWidth = 90.dp,
    ) { index ->
        val item = items.getOrNull(index)
        if (item != null) {
            LaunchedEffect(item) { state.model.ensureExtra(item) }
            val extra = state.model.extraOf(item)
            val line = extra?.progressLine().orEmpty()
            val info = carouselItemDrawInfo
            Box(
                Modifier
                    .fillMaxSize()
                    .maskClip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onOpen(item) }
            ) {
                AsyncImage(
                    model = item.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = info.textAlpha() }
                ) {
                    CinemaScrim()
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    ) {
                        Text(
                            item.title,
                            style = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (line.isNotEmpty()) {
                            Text(
                                line,
                                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                                color = Color.White.copy(alpha = .85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 一周 7 个等宽按钮「一 … 日」高 44 13sp SemiBold：选中 = primary / onPrimary 圆角 16，
 * 其他 = surfaceContainerHigh 全圆角（fastSpatialSpec）；未选中的「今天」文字用 primary。
 */
@Composable
private fun PhoneDayButtons(selected: Int, onSelect: (Int) -> Unit) {
    val names = stringArrayResource(R.array.timeline_weekday_short)
    val today = remember { todayDayOfWeek() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (day in 1..7) {
            val isSelected = day == selected
            val corner by animateDpAsState(
                if (isSelected) 16.dp else 22.dp,
                MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "pgcDayCorner",
            )
            val container by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "pgcDayContainer",
            )
            val content = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                day == today -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
            Surface(
                onClick = { onSelect(day) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(corner),
                color = container,
                contentColor = content,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        names.getOrElse(day - 1) { "" },
                        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

/** 分区 feed 行：SectionHeader + LazyRow PosterCard(width 112)。 */
@Composable
private fun PhoneFeedRow(
    title: String,
    posters: List<PgcItem>,
    onOpen: (PgcItem) -> Unit,
    onLoadMore: (() -> Unit)? = null,
) {
    val rowState = rememberLazyListState()
    if (onLoadMore != null) rowState.LoadMoreEffect { if (posters.isNotEmpty()) onLoadMore() }
    Column {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(posters.size, contentType = { "poster" }) { index ->
                val item = posters[index]
                PosterCard(data = item.toCard(), onClick = { onOpen(item) }, width = 112.dp)
            }
        }
    }
}
