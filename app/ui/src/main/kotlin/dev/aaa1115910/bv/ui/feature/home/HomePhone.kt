package dev.aaa1115910.bv.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemDrawInfo
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.adaptive.WidthClass
import dev.aaa1115910.bv.ui.designsystem.component.BvSearchField
import dev.aaa1115910.bv.ui.designsystem.component.BvTabRow
import dev.aaa1115910.bv.ui.designsystem.component.CinemaScrim
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.navigation.HistoryRoute
import dev.aaa1115910.bv.ui.navigation.LocalBottomBarPadding
import dev.aaa1115910.bv.ui.navigation.SearchRoute
import dev.aaa1115910.bv.ui.navigation.TopLevel
import dev.aaa1115910.bv.ui.navigation.navigateTopLevel
import dev.aaa1115910.bv.ui.navigation.openVideo
import kotlinx.coroutines.launch

/** 网格的垂直间距；整行项（区块标题、横滑行）的上边距按原型数值减去它。 */
private val PhoneGridGap = 12.dp

/**
 * 手机首页（原型 homePhone）：头像 + 搜索框、推荐 / 热门 / 分区三个标签、HorizontalPager。
 * 宽度 Medium（600–840dp）时网格用 3 列，否则 2 列。
 */
@Composable
internal fun HomePhone(nav: NavController, state: HomeState) {
    val columns = if (LocalDeviceProfile.current.width == WidthClass.Medium) 3 else 2
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.home_tab_recommend),
        stringResource(R.string.home_tab_popular),
        stringResource(R.string.home_tab_region),
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeAvatar(state, onClick = { nav.navigateTopLevel(TopLevel.Me) })
            BvSearchField(
                value = "",
                placeholder = state.searchPlaceholder(),
                modifier = Modifier.weight(1f),
                onClick = { nav.navigate(SearchRoute()) },
                onVoiceResult = { nav.navigate(SearchRoute(it)) },
            )
        }
        BvTabRow(
            tabs = tabs,
            selected = pager.currentPage,
            onSelect = { scope.launch { pager.animateScrollToPage(it) } },
        )
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            key = { it },
        ) { page ->
            when (page) {
                0 -> PhoneRecommendPage(nav, state, columns)
                1 -> PhonePopularPage(nav, state, columns)
                else -> PhoneRegionPage(nav, state, columns)
            }
        }
    }
}

@Composable
private fun PhoneRecommendPage(nav: NavController, state: HomeState, columns: Int) {
    val gridState = rememberLazyGridState()
    val vm = state.recommend
    val list = vm.recommendVideoList
    val bottom = LocalBottomBarPadding.current + 16.dp
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottom),
                onRetry = state::loadMoreRecommend,
            )
        } else {
            val featured = state.featured
            val continueWatching = state.continueWatching
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottom),
                horizontalArrangement = Arrangement.spacedBy(PhoneGridGap),
                verticalArrangement = Arrangement.spacedBy(PhoneGridGap),
            ) {
                if (featured.isNotEmpty()) {
                    item(key = "featured", span = { GridItemSpan(maxLineSpan) }, contentType = "featured") {
                        PhoneFeaturedCarousel(
                            items = featured,
                            onOpen = { nav.openVideo(it.toCard()) },
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
                if (continueWatching.isNotEmpty()) {
                    item(key = "continueHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        SectionHeader(
                            title = stringResource(R.string.home_continue_watching),
                            modifier = Modifier.padding(top = 16.dp - PhoneGridGap),
                            action = stringResource(R.string.home_all),
                            onAction = { nav.navigate(HistoryRoute) },
                        )
                    }
                    item(key = "continueRow", span = { GridItemSpan(maxLineSpan) }, contentType = "row") {
                        // 原型 padding-top 10；网格间距 12 已提供
                        ContinueRow(
                            items = continueWatching,
                            cardWidth = 150.dp,
                            spacing = 10.dp,
                            onOpen = { nav.openVideo(it) },
                            modifier = Modifier.fullBleed(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        )
                    }
                }
                item(key = "forYouHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    SectionHeader(
                        title = stringResource(R.string.home_for_you),
                        modifier = Modifier.padding(top = 14.dp - PhoneGridGap),
                    )
                }
                // 精选已经展示的前 5 条不再重复
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
private fun PhonePopularPage(nav: NavController, state: HomeState, columns: Int) {
    val gridState = rememberLazyGridState()
    val vm = state.popular
    val list = vm.popularVideoList
    val bottom = LocalBottomBarPadding.current + 16.dp
    LaunchedEffect(Unit) { state.ensurePopular() }
    gridState.LoadMoreEffect { if (list.isNotEmpty()) state.loadMorePopular() }

    PullToRefreshBox(
        isRefreshing = state.popularPulling,
        onRefresh = state::refreshPopular,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (list.isEmpty() && !state.popularPulling) {
            EmptyOrLoading(
                loading = vm.loading,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottom),
                onRetry = state::loadMorePopular,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = bottom),
                horizontalArrangement = Arrangement.spacedBy(PhoneGridGap),
                verticalArrangement = Arrangement.spacedBy(PhoneGridGap),
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

@Composable
private fun PhoneRegionPage(nav: NavController, state: HomeState, columns: Int) {
    val names = rememberRegionNames()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val bottom = LocalBottomBarPadding.current + 16.dp
    Column(Modifier.fillMaxSize()) {
        RegionChipRow(
            names = names,
            selected = selected,
            onSelect = { selected = it },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        )
        key(selected) {
            RegionGrid(
                nav = nav,
                state = state,
                type = HomeRegions[selected],
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottom),
                spacing = PhoneGridGap,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 某个分区的视频网格（手机 2 列 / 平板 Adaptive(180)），下拉刷新 + 快到底部加载更多。 */
@Composable
internal fun RegionGrid(
    nav: NavController,
    state: HomeState,
    type: UgcTypeV2,
    columns: GridCells,
    contentPadding: PaddingValues,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    val vm = regionViewModel(type)
    val gridState = rememberLazyGridState()
    var refreshing by remember { mutableStateOf(false) }
    val items = vm.ugcItems
    gridState.LoadMoreEffect { if (items.isNotEmpty()) state.loadMoreRegion(vm) }
    val refresh = {
        refreshing = true
        state.refreshRegion(vm) { refreshing = false }
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (items.isEmpty()) {
            EmptyOrLoading(
                loading = vm.updating || refreshing,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                onRetry = refresh,
            )
        } else {
            LazyVerticalGrid(
                columns = columns,
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                ugcCards(items, onOpen = { nav.openVideo(it) })
                if (vm.updating && !refreshing) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                        LoadMoreIndicator()
                    }
                }
            }
        }
    }
}

/** 网格里的视频卡（showMeta = true）。[skip] 跳过开头若干条（精选已展示）。 */
internal fun LazyGridScope.ugcCards(
    list: List<UgcItem>,
    skip: Int = 0,
    onOpen: (dev.aaa1115910.bv.entity.carddata.VideoCardData) -> Unit,
) {
    val count = (list.size - skip).coerceAtLeast(0)
    items(count, contentType = { "video" }) { index ->
        val item = list.getOrNull(index + skip)
        if (item != null) {
            val card = remember(item) { item.toCard() }
            VideoCard(data = card, onClick = { onOpen(card) })
        }
    }
}

/**
 * 精选多尺寸轮播（第 9.1 节）：高 196，preferredItemWidth 300，itemSpacing 8，小项 40–56。
 * 每项 maskClip 圆角 28，封面 + CinemaScrim + 左下「今日精选」+ 标题；小项里文字随宽度淡出。
 */
@Composable
private fun PhoneFeaturedCarousel(items: List<UgcItem>, onOpen: (UgcItem) -> Unit, modifier: Modifier = Modifier) {
    val count by rememberUpdatedState(items.size)
    val carouselState = rememberCarouselState { count }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 300.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp),
        itemSpacing = 8.dp,
        minSmallItemWidth = 40.dp,
        maxSmallItemWidth = 56.dp,
    ) { index ->
        val item = items.getOrNull(index)
        if (item != null) {
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
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            stringResource(R.string.home_featured),
                            style = BvType.Eyebrow,
                            color = Color.White.copy(alpha = .85f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.title,
                            style = BvType.HeroPhone,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** 小项（被压缩到 40–56dp）不显示文字层：按当前宽度在大项尺寸的 60%→100% 之间淡入。 */
private fun CarouselItemDrawInfo.textAlpha(): Float {
    val range = maxSize - minSize
    if (range <= 0f) return 1f
    val fraction = ((size - minSize) / range).coerceIn(0f, 1f)
    return ((fraction - 0.6f) / 0.4f).coerceIn(0f, 1f)
}
