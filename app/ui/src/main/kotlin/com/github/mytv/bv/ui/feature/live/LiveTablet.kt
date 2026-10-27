package com.github.mytv.bv.ui.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomItem
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.BvChip
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.timeline.SubPageLoginPrompt
import com.github.mytv.bv.ui.navigation.LoginRoute
import com.github.mytv.bv.ui.navigation.LivePlayerRoute
import com.github.mytv.bv.viewmodel.live.LiveHomeViewModel

private val TabletGridGap = 16.dp

/**
 * 平板直播页（对齐首页平板布局）：导航轨由外壳提供；左标题 + 标签 chips，下面是 Adaptive(180) 网格。
 */
@Composable
internal fun LiveTablet(nav: NavController, vm: LiveHomeViewModel, login: Boolean) {
    var tab by rememberSaveable { mutableIntStateOf(LiveTabRecommend) }
    val tabs = rememberLiveTabNames()
    val openRoom: (LiveRoomItem) -> Unit = { nav.navigate(LivePlayerRoute(it.roomId)) }

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
            Text(
                stringResource(R.string.nav_live),
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
                tabs.forEachIndexed { index, name ->
                    BvChip(text = name, selected = tab == index, onClick = { tab = index })
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (tab) {
                LiveTabRecommend -> {
                    LaunchedEffect(Unit) { vm.ensureRecommend() }
                    LiveRoomGridPage(
                        items = vm.recommendList,
                        loading = vm.recommendLoading,
                        columns = GridCells.Adaptive(180.dp),
                        onOpen = openRoom,
                        // 推荐接口没有分页参数，一次加载完
                        onLoadMore = {},
                        onRetry = vm::ensureRecommend,
                        contentPadding = PaddingValues(bottom = 24.dp),
                        spacing = TabletGridGap,
                        showLoadMore = false,
                    )
                }

                LiveTabFollowing -> {
                    LaunchedEffect(login) { vm.ensureFollowing(login) }
                    if (!login) {
                        SubPageLoginPrompt(onLogin = { nav.navigate(LoginRoute) })
                    } else {
                        LiveRoomGridPage(
                            items = vm.followingList,
                            loading = vm.followingLoading,
                            columns = GridCells.Adaptive(180.dp),
                            onOpen = openRoom,
                            // 关注接口没有分页参数，一次加载完
                            onLoadMore = {},
                            onRetry = { vm.ensureFollowing(true) },
                            contentPadding = PaddingValues(bottom = 24.dp),
                            spacing = TabletGridGap,
                            showLoadMore = false,
                        )
                    }
                }

                else -> TabletAreaPage(nav, vm)
            }
        }
    }
}

/** 平板分区页：一级分区 chips + 子分区 chips（含「全部」）+ Adaptive(180) 网格。 */
@Composable
private fun TabletAreaPage(nav: NavController, vm: LiveHomeViewModel) {
    LaunchedEffect(Unit) { vm.ensureAreaList() }
    val parentNames = vm.areaParents.map { it.name }
    val childNames = vm.areaParents.getOrNull(vm.selectedParent)?.list?.map { it.name }.orEmpty()
    val openRoom: (LiveRoomItem) -> Unit = { nav.navigate(LivePlayerRoute(it.roomId)) }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TabletGridGap),
    ) {
        LiveChipRow(
            names = parentNames,
            selected = vm.selectedParent,
            onSelect = { vm.selectArea(it, 0) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (childNames.isNotEmpty()) {
            LiveChipRow(
                names = childNames,
                selected = vm.selectedChild,
                onSelect = { vm.selectArea(vm.selectedParent, it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        key(vm.selectedParent, vm.selectedChild) {
            LiveRoomGridPage(
                items = vm.areaRooms,
                // 分区列表还在加载时房间必然为空，不能提前显示空提示
                loading = vm.areaRoomsLoading || (vm.areaParents.isEmpty() && vm.areaListLoading),
                columns = GridCells.Adaptive(180.dp),
                onOpen = openRoom,
                onLoadMore = vm::loadMoreAreaRooms,
                onRetry = {
                    if (vm.areaParents.isEmpty()) vm.ensureAreaList()
                    else vm.selectArea(vm.selectedParent, vm.selectedChild)
                },
                contentPadding = PaddingValues(bottom = 24.dp),
                spacing = TabletGridGap,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
