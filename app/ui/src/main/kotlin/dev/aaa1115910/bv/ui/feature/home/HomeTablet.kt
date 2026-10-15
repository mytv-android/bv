package dev.aaa1115910.bv.ui.feature.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvSearchField
import dev.aaa1115910.bv.ui.designsystem.component.CinemaScrim
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.navigation.HistoryRoute
import dev.aaa1115910.bv.ui.navigation.SearchRoute
import dev.aaa1115910.bv.ui.navigation.TopLevel
import dev.aaa1115910.bv.ui.navigation.navigateTopLevel
import dev.aaa1115910.bv.ui.navigation.openVideo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 平板网格间距；原型 m-col 的区块间距是 18，整行项再补 2dp。 */
private val TabletGridGap = 16.dp
private val TabletSectionExtra = 18.dp - TabletGridGap

private const val PageRecommend = 0
private const val PagePopular = 1
private const val PageRegion = 2

/**
 * 平板首页（原型 homeTablet）：导航轨由外壳提供；右侧 Column(padding start 8 / end 28 / top 20, spacedBy 18)：
 * 搜索框(440) + 标签（推荐 热门 分区 + 4 个快捷分区）+ 头像，下面是 Adaptive(180) 网格。
 */
@Composable
internal fun HomeTablet(nav: NavController, state: HomeState) {
    var page by rememberSaveable { mutableIntStateOf(PageRecommend) }
    var region by rememberSaveable { mutableIntStateOf(0) }
    val names = rememberRegionNames()
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvSearchField(
                value = "",
                placeholder = stringResource(R.string.home_search_placeholder_tablet),
                modifier = Modifier.width(440.dp),
                onClick = { nav.navigate(SearchRoute()) },
                onVoiceResult = { nav.navigate(SearchRoute(it)) },
            )
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BvChip(stringResource(R.string.home_tab_recommend), page == PageRecommend, { page = PageRecommend })
                BvChip(stringResource(R.string.home_tab_popular), page == PagePopular, { page = PagePopular })
                BvChip(stringResource(R.string.home_tab_region), page == PageRegion, { page = PageRegion })
                // 快捷分区：点击 = 选中「分区」页并切到该分区
                TabletQuickRegions.forEach { type ->
                    val index = HomeRegions.indexOf(type)
                    BvChip(
                        text = names[index],
                        selected = page == PageRegion && region == index,
                        onClick = {
                            page = PageRegion
                            region = index
                        },
                    )
                }
            }
            HomeAvatar(state, onClick = { nav.navigateTopLevel(TopLevel.Me) })
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (page) {
                PageRecommend -> TabletRecommendPage(nav, state)
                PagePopular -> TabletPopularPage(nav, state)
                else -> TabletRegionPage(nav, state, names, region, onRegion = { region = it })
            }
        }
    }
}

@Composable
private fun TabletRecommendPage(nav: NavController, state: HomeState) {
    val gridState = rememberLazyGridState()
    val vm = state.recommend
    val list = vm.recommendVideoList
    LaunchedEffect(Unit) { state.ensureRecommend() }
    gridState.LoadMoreEffect { if (list.isNotEmpty()) state.loadMoreRecommend() }

    PullToRefreshBox(
        isRefreshing = state.recommendPulling,
        onRefresh = state::refreshRecommend,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (list.isEmpty() && !state.recommendPulling) {
            EmptyOrLoading(
                loading = vm.loading || vm.refreshing,
                modifier = Modifier.fillMaxSize(),
                onRetry = state::loadMoreRecommend,
            )
        } else {
            val featured = state.featured
            val continueWatching = state.continueWatching
            LazyVerticalGrid(
                columns = GridCells.Adaptive(180.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(TabletGridGap),
                verticalArrangement = Arrangement.spacedBy(TabletGridGap),
            ) {
                if (featured.isNotEmpty()) {
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
                        TabletHero(featured, state, onOpen = { nav.openVideo(it.toCard()) })
                    }
                }
                if (continueWatching.isNotEmpty()) {
                    item(key = "continueHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        SectionHeader(
                            title = stringResource(R.string.home_continue_watching),
                            modifier = Modifier.padding(top = TabletSectionExtra),
                            action = stringResource(R.string.home_all),
                            onAction = { nav.navigate(HistoryRoute) },
                        )
                    }
                    item(key = "continueRow", span = { GridItemSpan(maxLineSpan) }, contentType = "row") {
                        ContinueRow(
                            items = continueWatching,
                            cardWidth = 196.dp,
                            spacing = 16.dp,
                            onOpen = { nav.openVideo(it) },
                            modifier = Modifier.padding(top = TabletSectionExtra),
                        )
                    }
                }
                item(key = "forYouHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    SectionHeader(
                        title = stringResource(R.string.home_for_you),
                        modifier = Modifier.padding(top = TabletSectionExtra),
                    )
                }
                ugcCards(list, skip = featured.size, onOpen = { nav.openVideo(it) })
                if (vm.loading) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                        LoadMoreIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletPopularPage(nav: NavController, state: HomeState) {
    val gridState = rememberLazyGridState()
    val vm = state.popular
    val list = vm.popularVideoList
    LaunchedEffect(Unit) { state.ensurePopular() }
    gridState.LoadMoreEffect { if (list.isNotEmpty()) state.loadMorePopular() }

    PullToRefreshBox(
        isRefreshing = state.popularPulling,
        onRefresh = state::refreshPopular,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (list.isEmpty() && !state.popularPulling) {
            EmptyOrLoading(loading = vm.loading, modifier = Modifier.fillMaxSize(), onRetry = state::loadMorePopular)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(180.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(TabletGridGap),
                verticalArrangement = Arrangement.spacedBy(TabletGridGap),
            ) {
                ugcCards(list, onOpen = { nav.openVideo(it) })
                if (vm.loading) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                        LoadMoreIndicator()
                    }
                }
            }
        }
    }
}

/** 分区页：31 个分区标签横向铺开（不折叠），下面是该分区的 Adaptive(180) 网格。 */
@Composable
private fun TabletRegionPage(
    nav: NavController,
    state: HomeState,
    names: List<String>,
    region: Int,
    onRegion: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(TabletGridGap)) {
        RegionChipRow(names = names, selected = region, onSelect = onRegion)
        key(region) {
            RegionGrid(
                nav = nav,
                state = state,
                type = HomeRegions[region],
                columns = GridCells.Adaptive(180.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                spacing = TabletGridGap,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 居中大图轮播（第 9.1 节）：HorizontalPager，高 300，contentPadding 水平 122，pageSpacing 12（两侧露出 110dp）。
 * 每页圆角 28；只有当前页显示文字层。每 6 秒自动翻页，用户触摸后暂停 10 秒。
 */
@Composable
private fun TabletHero(items: List<UgcItem>, state: HomeState, onOpen: (UgcItem) -> Unit) {
    val count by rememberUpdatedState(items.size)
    val pager = rememberPagerState { count }
    val scope = rememberCoroutineScope()
    var lastTouch by remember { mutableLongStateOf(0L) }

    LaunchedEffect(pager) {
        while (true) {
            val sinceTouch = SystemClock.uptimeMillis() - lastTouch
            delay(if (sinceTouch < 10_000L) 10_000L - sinceTouch else 6_000L)
            val idle = SystemClock.uptimeMillis() - lastTouch >= 10_000L
            if (idle && count > 1 && !pager.isScrollInProgress) {
                pager.animateScrollToPage((pager.currentPage + 1) % count)
            }
        }
    }
    val currentAid = items.getOrNull(pager.currentPage)?.aid
    LaunchedEffect(currentAid) { currentAid?.let(state::loadFeaturedExtra) }

    HorizontalPager(
        state = pager,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .pointerInput(Unit) {
                // 记录触摸时间（按下与抬起），用于暂停自动翻页
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    lastTouch = SystemClock.uptimeMillis()
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                    lastTouch = SystemClock.uptimeMillis()
                }
            },
        contentPadding = PaddingValues(horizontal = 122.dp),
        pageSpacing = 12.dp,
    ) { page ->
        val item = items.getOrNull(page)
        if (item != null) {
            val isCurrent = page == pager.currentPage
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        if (isCurrent) onOpen(item) else scope.launch { pager.animateScrollToPage(page) }
                    }
            ) {
                AsyncImage(
                    model = item.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                AnimatedVisibility(
                    visible = isCurrent,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                ) {
                    TabletHeroText(item, state, onOpen = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun TabletHeroText(item: UgcItem, state: HomeState, onOpen: () -> Unit) {
    val region = state.extraOf(item.aid)?.region
    val card = remember(item) { item.toCard() }
    val meta = listOfNotNull(
        card.upName.takeIf { it.isNotEmpty() },
        card.playString.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.home_play_count, it) },
        card.timeString.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")
    Box(Modifier.fillMaxSize()) {
        CinemaScrim()
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (region != null) stringResource(R.string.home_featured_region, region)
                else stringResource(R.string.home_featured),
                style = BvType.Eyebrow,
                color = Color.White.copy(alpha = .85f),
                maxLines = 1,
            )
            Text(
                item.title,
                style = BvType.HeroTablet,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    style = BvType.Small,
                    color = Color.White.copy(alpha = .85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BvButton(
                    text = stringResource(R.string.home_play),
                    onClick = onOpen,
                    icon = Icons.Filled.PlayArrow,
                )
                BvButton(
                    text = stringResource(R.string.home_watch_later),
                    onClick = { state.addToView(item.aid) },
                    style = BvButtonStyle.Tonal,
                    icon = Icons.Filled.WatchLater,
                )
            }
        }
    }
}
