package dev.aaa1115910.bv.ui.feature.pgc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.CinemaScrim
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PosterCard
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.feature.timeline.thumbnail
import dev.aaa1115910.bv.ui.feature.timeline.todayDayOfWeek
import dev.aaa1115910.bv.ui.navigation.FollowingSeasonRoute
import dev.aaa1115910.bv.ui.navigation.PgcIndexRoute
import dev.aaa1115910.bv.ui.navigation.PgcTimelineRoute
import kotlinx.coroutines.delay

/**
 * 平板影视（原型 pgcTablet）：标题行（「影视」+ 6 个标签 + 索引 / 我的追番）、
 * 横幅（weight 1，高 320）+「今天更新」侧卡（300）、「热门」6 列海报网格。
 */
@Composable
internal fun PgcTablet(nav: NavController, state: PgcUiState) {
    val names = pgcTabNames()
    val feed = state.feed
    val vm = feed.vm
    val filter = state.type.timelineFilter
    val gridState = rememberLazyGridState()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    var shownTab by rememberSaveable { mutableIntStateOf(state.tab) }
    LaunchedEffect(state.tab) {
        if (shownTab != state.tab) {
            shownTab = state.tab
            gridState.scrollToItem(0)
        }
    }
    gridState.LoadMoreEffect { if (feed.hot.isNotEmpty()) feed.loadMore() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 28.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.pgc_title),
                style = BvType.PageTitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                names.forEachIndexed { index, name ->
                    BvChip(text = name, selected = index == state.tab, onClick = { state.onTab(index) })
                }
            }
            BvButton(
                text = stringResource(R.string.pgc_index),
                onClick = { nav.navigate(PgcIndexRoute(state.type.ordinal)) },
                style = BvButtonStyle.Tonal,
                icon = Icons.Filled.Tune,
            )
            BvButton(
                text = stringResource(R.string.pgc_my_following),
                onClick = { nav.navigate(FollowingSeasonRoute) },
                style = BvButtonStyle.Tonal,
                icon = Icons.Filled.Star,
            )
        }

        if (feed.featured.isEmpty() && feed.hot.isEmpty()) {
            EmptyOrLoading(
                loading = vm.updating,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                message = stringResource(R.string.pgc_empty),
                onRetry = { vm.reloadAll() },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = bottom),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "hero", span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        TabletBanner(
                            items = feed.featured,
                            state = state,
                            nav = nav,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        if (filter != null) {
                            TabletTodayCard(
                                filter = filter,
                                state = state,
                                nav = nav,
                                modifier = Modifier
                                    .width(300.dp)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
                item(key = "hotHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    SectionHeader(
                        title = stringResource(R.string.pgc_hot, names[state.tab]),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                items(feed.hot.size, contentType = { "poster" }) { index ->
                    val item = feed.hot[index]
                    PosterCard(data = item.toCard(), onClick = { nav.openSeason(item.seasonId) })
                }
                if (vm.updating) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                        LoadMoreIndicator()
                    }
                }
            }
        }
    }
}

/** 横幅：圆角 28，横图 Crop + CinemaScrim；每 6 秒切换到下一条精选。 */
@Composable
private fun TabletBanner(
    items: List<CarouselData.CarouselItem>,
    state: PgcUiState,
    nav: NavController,
    modifier: Modifier,
) {
    if (items.isEmpty()) {
        PgcHeroPlaceholder(modifier)
    } else {
        val count by rememberUpdatedState(items.size)
        val pager = rememberPagerState { count }
        LaunchedEffect(pager) {
            while (true) {
                delay(6_000L)
                if (count > 1 && !pager.isScrollInProgress) {
                    pager.animateScrollToPage((pager.currentPage + 1) % count)
                }
            }
        }
        HorizontalPager(
            state = pager,
            modifier = modifier.clip(RoundedCornerShape(28.dp)),
        ) { page ->
            val item = items.getOrNull(page)
            if (item != null) TabletBannerPage(item, state, nav)
        }
    }
}

/** 横幅内容：左下 (28, 24) Column(spacedBy 8)：Eyebrow「本季热播 · {评分} 分」+ 标题 HeroTablet + 「看第 N 话」「追番」。 */
@Composable
private fun TabletBannerPage(item: CarouselData.CarouselItem, state: PgcUiState, nav: NavController) {
    LaunchedEffect(item) { state.model.ensureExtra(item) }
    val extra = state.model.extraOf(item)
    val rating = state.feed.ratingOf(extra?.seasonId ?: item.seasonId)
    val followed = state.follow.isFollowed(item, extra)
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { nav.openFeatured(item) }
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        CinemaScrim()
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (rating != null) stringResource(R.string.pgc_featured_rating, rating)
                else stringResource(R.string.pgc_featured),
                style = BvType.Eyebrow,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                item.title,
                style = BvType.HeroTablet,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BvButton(
                    text = watchLabel(extra),
                    onClick = { nav.openFeatured(item) },
                    icon = Icons.Filled.PlayArrow,
                )
                BvButton(
                    text = stringResource(if (followed) R.string.pgc_followed else R.string.pgc_follow),
                    onClick = { state.follow.toggle(item, extra) },
                    style = BvButtonStyle.Tonal,
                    icon = if (followed) Icons.Filled.Check else Icons.Filled.Add,
                )
            }
        }
    }
}

/** 侧卡：width 300 圆角 28 surfaceContainer padding 18：「今天更新」+「周X」，3 行（海报 44×58 + 标题 + 「HH:mm 更新」）。 */
@Composable
private fun TabletTodayCard(
    filter: TimelineFilter,
    state: PgcUiState,
    nav: NavController,
    modifier: Modifier,
) {
    val weekdays = stringArrayResource(R.array.timeline_weekday)
    val today = remember { todayDayOfWeek() }
    val episodes = state.model.timelinesOf(filter).episodesOf(today)
    Column(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            title = stringResource(R.string.pgc_today_update),
            action = weekdays.getOrElse(today - 1) { "" },
            onAction = { nav.navigate(PgcTimelineRoute) },
        )
        when {
            episodes.isEmpty() && state.model.isTimelineLoading(filter) -> Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }

            episodes.isEmpty() -> Text(
                stringResource(R.string.pgc_today_empty),
                style = BvType.Small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> episodes.take(3).forEach { episode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { nav.openSeason(episode.seasonId) },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = episode.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 44.dp, height = 58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            episode.title,
                            style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(R.string.pgc_update_at, episode.publishTime),
                            style = BvType.Meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
