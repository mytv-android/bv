package dev.aaa1115910.bv.ui.feature.live

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomItem
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PageMessage
import dev.aaa1115910.bv.ui.designsystem.focus.BvKey
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.onBvKey
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.feature.timeline.tryRequestFocus
import dev.aaa1115910.bv.ui.navigation.LivePlayerRoute
import dev.aaa1115910.bv.viewmodel.live.LiveHomeViewModel

/** LazyColumn/网格顶部留给顶栏标签的空间（同 HomeTv）。 */
private val TvContentTop = 74.dp

/**
 * 电视直播页（对齐 HomeTv）：顶栏标签（聚焦即选中）+ 4 列网格，TvColumnPivot 焦点枢轴滚动，
 * 内容滚到顶栏下面时隐藏顶栏；滚动到最后一行附近加载更多（LoadMoreEffect 对遥控器滚动同样生效）。
 * 「分区」页 = 一级分区 / 子分区两行 chips + 网格。
 */
@Composable
internal fun LiveTv(nav: NavController, vm: LiveHomeViewModel, login: Boolean) {
    var chip by rememberSaveable { mutableIntStateOf(LiveTabRecommend) }
    val recommendGrid = rememberLazyGridState()
    val followingGrid = rememberLazyGridState()
    val areaGrid = rememberLazyGridState()
    var chipsFocused by remember { mutableStateOf(false) }
    // 每次进入页面（含从直播间返回）只做一次初始焦点
    var entryPending by remember { mutableStateOf(true) }
    val chipRequesters = remember { List(LiveTabCount) { FocusRequester() } }

    LaunchedEffect(chip, login) {
        when (chip) {
            LiveTabRecommend -> vm.ensureRecommend()
            LiveTabFollowing -> vm.ensureFollowing(login)
            else -> vm.ensureAreaList()
        }
    }

    val topThreshold = with(LocalDensity.current) { 8.dp.roundToPx() }
    val atTop by remember {
        derivedStateOf {
            when (chip) {
                LiveTabRecommend -> recommendGrid.isNearTop(topThreshold)
                LiveTabFollowing -> followingGrid.isNearTop(topThreshold)
                else -> areaGrid.isNearTop(topThreshold)
            }
        }
    }
    // 内容滚动到顶栏下面时隐藏顶栏，避免与卡片重叠；焦点在标签上时始终显示
    val barAlpha by animateFloatAsState(
        if (atTop || chipsFocused) 1f else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "liveTvTopBar",
    )

    val openRoom: (LiveRoomItem) -> Unit = { nav.navigate(LivePlayerRoute(it.roomId)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (chip) {
            LiveTabRecommend -> TvGridContent(
                gridState = recommendGrid,
                items = vm.recommendList,
                loading = vm.recommendLoading,
                onOpen = openRoom,
                // 推荐接口没有分页参数，一次加载完
                onLoadMore = {},
                onRetry = vm::ensureRecommend,
                entryPending = entryPending,
                fallbackRequester = chipRequesters[LiveTabRecommend],
                onEntryDone = { entryPending = false },
            )

            LiveTabFollowing -> if (!login) {
                PageMessage(message = stringResource(R.string.live_login_required))
                LaunchedEffect(entryPending) {
                    if (entryPending) {
                        withFrameNanos { }
                        chipRequesters[LiveTabFollowing].tryRequestFocus()
                        entryPending = false
                    }
                }
            } else {
                TvGridContent(
                    gridState = followingGrid,
                    items = vm.followingList,
                    loading = vm.followingLoading,
                    onOpen = openRoom,
                    // 关注接口没有分页参数，一次加载完
                    onLoadMore = {},
                    onRetry = { vm.ensureFollowing(true) },
                    entryPending = entryPending,
                    fallbackRequester = chipRequesters[LiveTabFollowing],
                    onEntryDone = { entryPending = false },
                )
            }

            else -> TvAreaContent(
                nav = nav,
                vm = vm,
                gridState = areaGrid,
                entryPending = entryPending,
                fallbackRequester = chipRequesters[LiveTabArea],
                onEntryDone = { entryPending = false },
            )
        }

        TvTopBar(
            chip = chip,
            requesters = chipRequesters,
            onSelect = { chip = it },
            onReselect = {
                // 同电视首页：在已选中的标签上按确认键刷新该页
                when (it) {
                    LiveTabRecommend -> vm.refreshRecommend()
                    LiveTabFollowing -> vm.refreshFollowing(login)
                    else -> vm.selectArea(vm.selectedParent, vm.selectedChild)
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

private fun LazyGridState.isNearTop(threshold: Int) =
    firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset <= threshold

/** 顶栏 at (left 96, top 24, right 48)：标签（聚焦即选中）。 */
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
    val labels = rememberLiveTabNames()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 96.dp, top = 24.dp, end = 48.dp)
            .graphicsLayer { this.alpha = alpha() },
        horizontalArrangement = Arrangement.Start,
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
    }
}

/** 「推荐」「关注」：4 列网格（对齐电视首页热门/关注）。 */
@Composable
private fun TvGridContent(
    gridState: LazyGridState,
    items: List<LiveRoomItem>,
    loading: Boolean,
    onOpen: (LiveRoomItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    entryPending: Boolean,
    fallbackRequester: FocusRequester,
    onEntryDone: () -> Unit,
) {
    val gridRequester = remember { FocusRequester() }
    gridState.LoadMoreEffect { if (items.isNotEmpty()) onLoadMore() }
    LaunchedEffect(entryPending, items.isNotEmpty()) {
        if (!entryPending) return@LaunchedEffect
        if (items.isEmpty() && loading) return@LaunchedEffect
        withFrameNanos { }
        if (!(items.isNotEmpty() && gridRequester.tryRequestFocus())) fallbackRequester.tryRequestFocus()
        onEntryDone()
    }
    if (items.isEmpty()) {
        EmptyOrLoading(
            loading = loading,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 96.dp, top = TvContentTop),
            onRetry = onRetry,
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
            liveCards(items, onOpen)
            if (loading) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}

/** 「分区」：一级分区 / 子分区两行 chips + 4 列网格，触底加载更多。 */
@Composable
private fun TvAreaContent(
    nav: NavController,
    vm: LiveHomeViewModel,
    gridState: LazyGridState,
    entryPending: Boolean,
    fallbackRequester: FocusRequester,
    onEntryDone: () -> Unit,
) {
    val parentNames = vm.areaParents.map { it.name }
    val childNames = vm.areaParents.getOrNull(vm.selectedParent)?.list?.map { it.name }.orEmpty()
    val openRoom: (LiveRoomItem) -> Unit = { nav.navigate(LivePlayerRoute(it.roomId)) }
    val gridRequester = remember { FocusRequester() }
    gridState.LoadMoreEffect { vm.loadMoreAreaRooms() }

    LaunchedEffect(entryPending, vm.areaRooms.isNotEmpty()) {
        if (!entryPending) return@LaunchedEffect
        if (vm.areaRooms.isEmpty() && vm.areaRoomsLoading) return@LaunchedEffect
        withFrameNanos { }
        if (!(vm.areaRooms.isNotEmpty() && gridRequester.tryRequestFocus())) fallbackRequester.tryRequestFocus()
        onEntryDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = TvContentTop)
    ) {
        LiveChipRow(
            names = parentNames,
            selected = vm.selectedParent,
            onSelect = { vm.selectArea(it, 0) },
            // 视口从导航轨右边缘（收起 76dp）开始，标签不会滑到侧栏下面
            modifier = Modifier.padding(start = 76.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 48.dp, top = 12.dp, bottom = 6.dp),
        )
        if (childNames.isNotEmpty()) {
            LiveChipRow(
                names = childNames,
                selected = vm.selectedChild,
                onSelect = { vm.selectArea(vm.selectedParent, it) },
                modifier = Modifier.padding(start = 76.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 48.dp, bottom = 6.dp),
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (vm.areaRooms.isEmpty()) {
                EmptyOrLoading(
                    // 分区列表还在加载时房间必然为空，不能提前显示空提示
                    loading = vm.areaRoomsLoading || (vm.areaParents.isEmpty() && vm.areaListLoading),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 96.dp),
                    onRetry = {
                        if (vm.areaParents.isEmpty()) vm.ensureAreaList()
                        else vm.selectArea(vm.selectedParent, vm.selectedChild)
                    },
                )
            } else {
                TvColumnPivot {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(gridRequester)
                            .focusRestorer()
                            .focusGroup(),
                        contentPadding = PaddingValues(start = 102.dp, end = 48.dp, top = 12.dp, bottom = 27.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        liveCards(vm.areaRooms, openRoom)
                        if (vm.areaRoomsLoading) {
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") { LoadMoreIndicator() }
                        }
                    }
                }
            }
        }
    }
}
