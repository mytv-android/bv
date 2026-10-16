package dev.aaa1115910.bv.ui.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PageMessage
import dev.aaa1115910.bv.ui.designsystem.component.TvBackdrop
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.focus.BvKey
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.TvRowPivot
import dev.aaa1115910.bv.ui.designsystem.focus.onBvKey
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.navigation.openVideo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ChipRecommend = 0
private const val ChipPopular = 1
private const val ChipRegion = 2
private const val ChipFollowing = 3

/** 「推荐」页里分区行的个数（前 5 个分区）。 */
private const val TvRecommendRegionRows = 5

/** LazyColumn 顶部留给顶栏标签的空间。 */
private val TvContentTop = 74.dp

/**
 * 电视首页（原型 homeTv）：TvBackdrop(当前精选封面) + 顶栏标签（聚焦即选中）与时钟 + 内容。
 * 「推荐」：精选信息区（固定 266dp，「播放」为初始焦点，每 8 秒切换）+ 为你推荐 / 热门 / 前 5 个分区各一行。
 * 「热门」「关注」：4 列网格；「分区」：31 个分区各一行（原型未画，照旧电视版：热门/动态为网格，分区按分区浏览）。
 */
@Composable
internal fun HomeTv(nav: NavController, state: HomeState) {
    var chip by rememberSaveable { mutableIntStateOf(ChipRecommend) }
    val recommendList = rememberLazyListState()
    val popularGrid = rememberLazyGridState()
    val regionList = rememberLazyListState()
    val followingGrid = rememberLazyGridState()
    var chipsFocused by remember { mutableStateOf(false) }
    // 每次进入页面（含从详情返回）只做一次初始焦点
    var entryPending by remember { mutableStateOf(true) }
    val chipRequesters = remember { List(4) { FocusRequester() } }

    val featured = state.featured
    var featuredIndex by rememberSaveable { mutableIntStateOf(0) }
    val current = if (featured.isEmpty()) null else featured[featuredIndex % featured.size]

    LaunchedEffect(Unit) {
        state.ensureRecommend()
        state.ensurePopular()
    }

    val topThreshold = with(LocalDensity.current) { 8.dp.roundToPx() }
    val atTop by remember {
        derivedStateOf {
            when (chip) {
                ChipRecommend -> recommendList.isNearTop(topThreshold)
                ChipPopular -> popularGrid.isNearTop(topThreshold)
                ChipRegion -> regionList.isNearTop(topThreshold)
                else -> followingGrid.isNearTop(topThreshold)
            }
        }
    }
    // 内容滚动到顶栏下面时隐藏顶栏，避免与卡片重叠；焦点在标签上时始终显示
    val barAlpha by animateFloatAsState(
        if (atTop || chipsFocused) 1f else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tvTopBar",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (chip == ChipRecommend) TvBackdrop(cover = current?.cover)

        when (chip) {
            ChipRecommend -> TvRecommendContent(
                nav = nav,
                state = state,
                listState = recommendList,
                featuredIndex = featuredIndex,
                onFeaturedIndex = { featuredIndex = it },
                entryPending = entryPending,
                onEntryDone = { entryPending = false },
            )

            ChipPopular -> TvGridContent(
                gridState = popularGrid,
                cards = rememberCards(state.popular.popularVideoList) { it.toCard() },
                loading = state.popular.loading,
                onOpen = { nav.openVideo(it) },
                onLoadMore = state::loadMorePopular,
                entryPending = entryPending,
                fallbackRequester = chipRequesters[ChipPopular],
                onEntryDone = { entryPending = false },
            )

            ChipRegion -> TvRegionContent(
                nav = nav,
                state = state,
                listState = regionList,
                entryPending = entryPending,
                fallbackRequester = chipRequesters[ChipRegion],
                onEntryDone = { entryPending = false },
            )

            else -> {
                LaunchedEffect(Unit) { state.ensureDynamic() }
                if (!state.dynamic.isLogin) {
                    PageMessage(message = stringResource(R.string.home_login_required))
                    LaunchedEffect(entryPending) {
                        if (entryPending) {
                            withFrameNanos { }
                            chipRequesters[ChipFollowing].tryFocus()
                            entryPending = false
                        }
                    }
                } else {
                    TvGridContent(
                        gridState = followingGrid,
                        cards = rememberCards(state.dynamic.dynamicVideoList) { it.toCard() },
                        loading = state.dynamic.loadingVideo,
                        onOpen = { nav.openVideo(it) },
                        onLoadMore = state::loadMoreDynamic,
                        entryPending = entryPending,
                        fallbackRequester = chipRequesters[ChipFollowing],
                        onEntryDone = { entryPending = false },
                    )
                }
            }
        }

        TvTopBar(
            chip = chip,
            requesters = chipRequesters,
            onSelect = { chip = it },
            onReselect = {
                // 同旧电视版：在已选中的标签上按确认键刷新该页
                when (it) {
                    ChipRecommend -> state.refreshRecommend()
                    ChipPopular -> state.refreshPopular()
                    ChipFollowing -> state.refreshDynamic()
                }
            },
            onDown = { entryPending = true },
            alpha = { barAlpha },
            onFocusChanged = {
                chipsFocused = it
                // 用户已经自己把焦点移到标签上，不再做初始焦点
                if (it) entryPending = false
            },
        )
    }
}

private fun LazyListState.isNearTop(threshold: Int) =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset <= threshold

private fun LazyGridState.isNearTop(threshold: Int) =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset <= threshold

/** requestFocus 不抛异常且成功时返回 true。 */
private fun FocusRequester.tryFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/** 顶栏 at (left 96, top 24, right 48)：标签（聚焦即选中）+ 时钟。 */
@Composable
private fun TvTopBar(
    chip: Int,
    requesters: List<FocusRequester>,
    onSelect: (Int) -> Unit,
    onReselect: (Int) -> Unit,
    onDown: () -> Unit,
    alpha: () -> Float,
    onFocusChanged: (Boolean) -> Unit,
) {
    val labels = listOf(
        stringResource(R.string.home_tab_recommend),
        stringResource(R.string.home_tab_popular),
        stringResource(R.string.home_tab_region),
        stringResource(R.string.home_tab_following),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 96.dp, top = 24.dp, end = 48.dp)
            .graphicsLayer { this.alpha = alpha() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .onFocusChanged { onFocusChanged(it.hasFocus) }
                // ↓ 从标签进入内容：交给内容侧的焦点请求器，默认焦点搜索在网格里会丢焦点
                .onBvKey { key -> if (key == BvKey.Down) { onDown(); true } else false }
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEachIndexed { index, label ->
                BvChip(
                    text = label,
                    selected = chip == index,
                    onClick = { if (chip == index) onReselect(index) else onSelect(index) },
                    modifier = Modifier.focusRequester(requesters[index]),
                    selectOnFocus = true,
                )
            }
        }
        TvClock()
    }
}

/** 时钟「HH:mm」15sp SemiBold onSurfaceVariant，每分钟刷新。 */
@Composable
private fun TvClock() {
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
        style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 「推荐」页的纵向列表滚动：0.30 枢轴（同 TvColumnPivot），但焦点在精选信息区时回到顶部，
 * 否则聚焦「播放」会把精选标题滚到顶栏下面。
 */
private class TvHomeColumnSpec(private val topLock: () -> Float?) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        topLock() ?: (offset - containerSize * 0.30f)
}

@Composable
private fun TvRecommendContent(
    nav: NavController,
    state: HomeState,
    listState: LazyListState,
    featuredIndex: Int,
    onFeaturedIndex: (Int) -> Unit,
    entryPending: Boolean,
    onEntryDone: () -> Unit,
) {
    val featured = state.featured
    val recommend = state.recommend.recommendVideoList
    val popular = state.popular.popularVideoList
    val playRequester = remember { FocusRequester() }
    // 行焦点请求器：索引 = LazyColumn 的 item 索引（0 是精选信息区）
    val rowRequesters = remember { List(3 + TvRecommendRegionRows) { FocusRequester() } }
    var savedRow by rememberSaveable { mutableIntStateOf(0) }
    var focusInFeatured by remember { mutableStateOf(false) }
    val spec = remember(listState) {
        TvHomeColumnSpec {
            if (focusInFeatured && listState.firstVisibleItemIndex == 0) {
                -listState.firstVisibleItemScrollOffset.toFloat()
            } else null
        }
    }

    // 精选每 8 秒切到下一条；焦点在精选区时不切换
    LaunchedEffect(focusInFeatured, featured.size) {
        if (focusInFeatured || featured.size < 2) return@LaunchedEffect
        var index = featuredIndex
        while (true) {
            delay(8_000L)
            index = (index + 1) % featured.size
            onFeaturedIndex(index)
        }
    }

    if (featured.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 96.dp)
        ) {
            EmptyOrLoading(
                loading = state.recommend.loading || state.recommend.refreshing,
                modifier = Modifier.fillMaxSize(),
                onRetry = state::loadMoreRecommend,
            )
        }
        return
    }
    val current = featured[featuredIndex % featured.size]

    // 初始焦点：「播放」；从详情返回时回到之前所在的行；在标签行按 ↓ 时也重新进入内容
    LaunchedEffect(entryPending) {
        if (!entryPending) return@LaunchedEffect
        withFrameNanos { }
        val row = savedRow
        val restored = row > 0 &&
            listState.layoutInfo.visibleItemsInfo.any { it.index == row } &&
            rowRequesters[row].tryFocus()
        if (!restored) {
            if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
                listState.scrollToItem(0)
                withFrameNanos { }
            }
            playRequester.tryFocus()
        }
        onEntryDone()
    }

    TvColumnPivot {
        CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = TvContentTop, bottom = 27.dp),
            ) {
                item(key = "featured", contentType = "featured") {
                    TvFeaturedBlock(
                        item = current,
                        state = state,
                        playRequester = playRequester,
                        onOpen = { nav.openVideo(current.toCard()) },
                        modifier = Modifier.onFocusChanged {
                            focusInFeatured = it.hasFocus
                            if (it.hasFocus) savedRow = 0
                        },
                    )
                }
                item(key = "forYou", contentType = "row") {
                    // 精选已展示的前 5 条不再重复
                    TvVideoRow(
                        title = stringResource(R.string.home_for_you),
                        hint = stringResource(R.string.home_more_rows_hint),
                        cards = rememberCards(recommend, skip = featured.size) { it.toCard() },
                        loading = state.recommend.loading,
                        onOpen = { nav.openVideo(it) },
                        onLoadMore = state::loadMoreRecommend,
                        requester = rowRequesters[1],
                        onFocused = { savedRow = 1 },
                    )
                }
                item(key = "popular", contentType = "row") {
                    TvVideoRow(
                        title = stringResource(R.string.home_tab_popular),
                        cards = rememberCards(popular) { it.toCard() },
                        loading = state.popular.loading,
                        onOpen = { nav.openVideo(it) },
                        onLoadMore = state::loadMorePopular,
                        requester = rowRequesters[2],
                        onFocused = { savedRow = 2 },
                    )
                }
                items(TvRecommendRegionRows, key = { "region$it" }, contentType = { "row" }) { index ->
                    TvRegionRow(
                        nav = nav,
                        state = state,
                        type = HomeRegions[index],
                        requester = rowRequesters[3 + index],
                        onFocused = { savedRow = 3 + index },
                    )
                }
            }
        }
    }
}

/**
 * 精选信息区：Column(start 96, width 430, spacedBy 8)，整块高 266。
 * 「今日精选 · 分区」、标题（Display，2 行）、「播放 · 时长 · UP」、简介（按需取，取不到隐藏）、按钮行。
 */
@Composable
private fun TvFeaturedBlock(
    item: UgcItem,
    state: HomeState,
    playRequester: FocusRequester,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(item.aid) { state.loadFeaturedExtra(item.aid) }
    Column(
        modifier
            .padding(start = 96.dp)
            .width(430.dp)
            .height(266.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Crossfade(
            targetState = item,
            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            label = "tvFeaturedText",
        ) { shown ->
            TvFeaturedText(shown, state)
        }
        Row(
            Modifier
                .padding(top = 6.dp)
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BvButton(
                text = stringResource(R.string.home_play),
                onClick = onOpen,
                modifier = Modifier.focusRequester(playRequester),
                icon = Icons.Filled.PlayArrow,
                large = true,
            )
            BvButton(
                text = stringResource(R.string.home_watch_later),
                onClick = { state.addToView(item.aid) },
                style = BvButtonStyle.Tonal,
                icon = Icons.Filled.WatchLater,
                large = true,
            )
            BvIconButton(
                icon = Icons.Filled.ThumbUp,
                contentDescription = stringResource(R.string.home_like),
                onClick = { state.toggleLike(item.aid) },
                style = if (state.isLiked(item.aid)) BvIconButtonStyle.Filled else BvIconButtonStyle.Tonal,
                size = 52.dp,
            )
        }
    }
}

@Composable
private fun TvFeaturedText(item: UgcItem, state: HomeState) {
    val extra = state.extraOf(item.aid)
    val card = remember(item) { item.toCard() }
    val meta = listOfNotNull(
        card.playString.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.home_play_count, it) },
        card.timeString.takeIf { it.isNotEmpty() },
        card.upName.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (extra?.region != null) stringResource(R.string.home_featured_region, extra.region)
            else stringResource(R.string.home_featured),
            style = BvType.Eyebrow,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Text(
            item.title,
            style = BvType.Display,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // 画质标签（4K/HDR）：推荐数据里没有画质信息，不显示
        if (meta.isNotEmpty()) {
            Text(
                meta,
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        extra?.description?.let { description ->
            Text(
                description,
                style = TextStyle(fontSize = 13.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 内容行：Row(start 96, end 48) 标题 15sp +（第一行）提示；
 * TvRowPivot { LazyRow(contentPadding start 102 / end 48 / top 12 / bottom 16, spacedBy 18, focusRestorer + focusGroup) }，
 * VideoCard(width 180, showMeta = false)。
 */
@Composable
private fun TvVideoRow(
    title: String,
    cards: List<VideoCardData>,
    loading: Boolean,
    onOpen: (VideoCardData) -> Unit,
    onLoadMore: () -> Unit,
    requester: FocusRequester,
    onFocused: () -> Unit,
    hint: String? = null,
) {
    if (cards.isEmpty() && !loading) return
    val rowState = rememberLazyListState()
    rowState.LoadMoreEffect { if (cards.isNotEmpty()) onLoadMore() }
    Column(Modifier.onFocusChanged { if (it.hasFocus) onFocused() }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 96.dp, end = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = BvType.Section,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (hint != null) {
                Text(hint, style = BvType.Label, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .padding(start = 102.dp, top = 12.dp, bottom = 16.dp)
                    .height(156.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                LoadingIndicator()
            }
        } else {
            TvRowPivot {
                LazyRow(
                    state = rowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(requester)
                        .focusRestorer()
                        .focusGroup(),
                    contentPadding = PaddingValues(start = 102.dp, end = 48.dp, top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(cards.size, contentType = { "video" }) { index ->
                        val card = cards[index]
                        VideoCard(
                            data = card,
                            onClick = { onOpen(card) },
                            width = 180.dp,
                            showMeta = false,
                        )
                    }
                }
            }
        }
    }
}

/** 一个分区一行（标题为分区名），数据来自对应的 Ugc*ViewModel。 */
@Composable
private fun TvRegionRow(
    nav: NavController,
    state: HomeState,
    type: UgcTypeV2,
    requester: FocusRequester,
    onFocused: () -> Unit,
) {
    val vm = regionViewModel(type)
    val names = rememberRegionNames()
    TvVideoRow(
        title = names[HomeRegions.indexOf(type)],
        cards = rememberCards(vm.ugcItems) { it.toCard() },
        loading = vm.updating,
        onOpen = { nav.openVideo(it) },
        onLoadMore = { state.loadMoreRegion(vm) },
        requester = requester,
        onFocused = onFocused,
    )
}

/** 「分区」标签：31 个分区各一行。 */
@Composable
private fun TvRegionContent(
    nav: NavController,
    state: HomeState,
    listState: LazyListState,
    entryPending: Boolean,
    fallbackRequester: FocusRequester,
    onEntryDone: () -> Unit,
) {
    val rowRequesters = remember { List(HomeRegions.size) { FocusRequester() } }
    var savedRow by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(entryPending) {
        if (!entryPending) return@LaunchedEffect
        withFrameNanos { }
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == savedRow }
        if (!(visible && rowRequesters[savedRow].tryFocus())) fallbackRequester.tryFocus()
        onEntryDone()
    }
    TvColumnPivot {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = TvContentTop, bottom = 27.dp),
        ) {
            items(HomeRegions.size, key = { HomeRegions[it].name }, contentType = { "row" }) { index ->
                TvRegionRow(
                    nav = nav,
                    state = state,
                    type = HomeRegions[index],
                    requester = rowRequesters[index],
                    onFocused = { savedRow = index },
                )
            }
        }
    }
}

/** 「热门」「关注」：4 列网格（旧电视版同为 4 列），VideoCard(showMeta = true)。 */
@Composable
private fun TvGridContent(
    gridState: LazyGridState,
    cards: List<VideoCardData>,
    loading: Boolean,
    onOpen: (VideoCardData) -> Unit,
    onLoadMore: () -> Unit,
    entryPending: Boolean,
    fallbackRequester: FocusRequester,
    onEntryDone: () -> Unit,
) {
    val gridRequester = remember { FocusRequester() }
    gridState.LoadMoreEffect { if (cards.isNotEmpty()) onLoadMore() }
    LaunchedEffect(entryPending, cards.isNotEmpty()) {
        if (!entryPending) return@LaunchedEffect
        if (cards.isEmpty() && loading) return@LaunchedEffect
        withFrameNanos { }
        if (!(cards.isNotEmpty() && gridRequester.tryFocus())) fallbackRequester.tryFocus()
        onEntryDone()
    }
    if (cards.isEmpty()) {
        EmptyOrLoading(
            loading = loading,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 96.dp, top = TvContentTop),
            onRetry = onLoadMore,
        )
        return
    }
    TvColumnPivot {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(gridRequester)
                .focusRestorer()
                .focusGroup(),
            contentPadding = PaddingValues(start = 102.dp, end = 48.dp, top = TvContentTop + 12.dp, bottom = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(cards.size, contentType = { "video" }) { index ->
                val card = cards[index]
                VideoCard(data = card, onClick = { onOpen(card) })
            }
            if (loading) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}
