package com.github.mytv.bv.ui.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomItem
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.adaptive.WidthClass
import com.github.mytv.bv.ui.designsystem.component.BvTabRow
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.timeline.SubPageLoginPrompt
import com.github.mytv.bv.ui.navigation.LocalBottomBarPadding
import com.github.mytv.bv.ui.navigation.LoginRoute
import com.github.mytv.bv.ui.navigation.LivePlayerRoute
import com.github.mytv.bv.viewmodel.live.LiveHomeViewModel
import kotlinx.coroutines.launch

/** 网格的垂直/水平间距。 */
private val PhoneGridGap = 12.dp

/**
 * 手机直播页（对齐首页手机布局）：页标题 + 推荐 / 关注 / 分区三个标签 + HorizontalPager。
 * 宽度 Medium（600–840dp）时网格用 3 列，否则 2 列。
 */
@Composable
internal fun LivePhone(nav: NavController, vm: LiveHomeViewModel, login: Boolean) {
    val columns = if (LocalDeviceProfile.current.width == WidthClass.Medium) 3 else 2
    val pager = rememberPagerState { LiveTabCount }
    val scope = rememberCoroutineScope()
    val tabs = rememberLiveTabNames()
    val bottom = LocalBottomBarPadding.current + 16.dp
    val openRoom: (LiveRoomItem) -> Unit = { nav.navigate(LivePlayerRoute(it.roomId)) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Text(
            stringResource(R.string.nav_live),
            style = BvType.PageTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
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
                LiveTabRecommend -> {
                    LaunchedEffect(Unit) { vm.ensureRecommend() }
                    PhoneGrid(
                        items = vm.recommendList,
                        loading = vm.recommendLoading,
                        columns = columns,
                        bottom = bottom,
                        onOpen = openRoom,
                        // 推荐接口没有分页参数，一次加载完
                        onLoadMore = {},
                        onRetry = vm::ensureRecommend,
                        showLoadMore = false,
                    )
                }

                LiveTabFollowing -> {
                    LaunchedEffect(login) { vm.ensureFollowing(login) }
                    if (!login) {
                        SubPageLoginPrompt(
                            onLogin = { nav.navigate(LoginRoute) },
                            modifier = Modifier.padding(bottom = bottom),
                        )
                    } else {
                        PhoneGrid(
                            items = vm.followingList,
                            loading = vm.followingLoading,
                            columns = columns,
                            bottom = bottom,
                            onOpen = openRoom,
                            // 关注接口没有分页参数，一次加载完
                            onLoadMore = {},
                            onRetry = { vm.ensureFollowing(true) },
                            showLoadMore = false,
                        )
                    }
                }

                else -> PhoneAreaPage(nav, vm, columns, bottom)
            }
        }
    }
}

@Composable
private fun PhoneGrid(
    items: List<LiveRoomItem>,
    loading: Boolean,
    columns: Int,
    bottom: Dp,
    onOpen: (LiveRoomItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    showLoadMore: Boolean = true,
) {
    LiveRoomGridPage(
        items = items,
        loading = loading,
        columns = GridCells.Fixed(columns),
        onOpen = onOpen,
        onLoadMore = onLoadMore,
        onRetry = onRetry,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = bottom),
        spacing = PhoneGridGap,
        showLoadMore = showLoadMore,
    )
}

/** 分区页：一级分区 chips + 子分区 chips（含「全部」）+ 房间网格，触底加载更多。 */
@Composable
private fun PhoneAreaPage(nav: NavController, vm: LiveHomeViewModel, columns: Int, bottom: Dp) {
    LaunchedEffect(Unit) { vm.ensureAreaList() }
    val parentNames = vm.areaParents.map { it.name }
    val childNames = vm.areaParents.getOrNull(vm.selectedParent)?.list?.map { it.name }.orEmpty()
    val openRoom: (LiveRoomItem) -> Unit = { nav.navigate(LivePlayerRoute(it.roomId)) }

    Column(Modifier.fillMaxSize()) {
        LiveChipRow(
            names = parentNames,
            selected = vm.selectedParent,
            onSelect = { vm.selectArea(it, 0) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        )
        if (childNames.isNotEmpty()) {
            LiveChipRow(
                names = childNames,
                selected = vm.selectedChild,
                onSelect = { vm.selectArea(vm.selectedParent, it) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
        key(vm.selectedParent, vm.selectedChild) {
            LiveRoomGridPage(
                items = vm.areaRooms,
                // 分区列表还在加载时房间必然为空，不能提前显示空提示
                loading = vm.areaRoomsLoading || (vm.areaParents.isEmpty() && vm.areaListLoading),
                columns = GridCells.Fixed(columns),
                onOpen = openRoom,
                onLoadMore = vm::loadMoreAreaRooms,
                onRetry = {
                    if (vm.areaParents.isEmpty()) vm.ensureAreaList()
                    else vm.selectArea(vm.selectedParent, vm.selectedChild)
                },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottom),
                spacing = PhoneGridGap,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
