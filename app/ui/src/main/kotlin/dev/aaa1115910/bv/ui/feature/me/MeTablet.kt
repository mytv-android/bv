package dev.aaa1115910.bv.ui.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvListItem
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.accounts.AccountsContent
import dev.aaa1115910.bv.ui.feature.favorite.FavoriteContent
import dev.aaa1115910.bv.ui.feature.following.FollowingContent
import dev.aaa1115910.bv.ui.feature.following.FollowingSeasonContent
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.feature.timeline.SubGridSpec
import dev.aaa1115910.bv.ui.feature.timeline.SubPageLoginPrompt
import dev.aaa1115910.bv.ui.feature.toview.ToViewContent
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.ui.navigation.SpaceRoute
import dev.aaa1115910.bv.ui.navigation.UserLockRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.user.FollowViewModel
import dev.aaa1115910.bv.viewmodel.user.FollowingSeasonViewModel
import org.koin.androidx.compose.koinViewModel

/** 右栏网格：不再额外留边（右栏自带 padding 24），间距 16。 */
private fun paneGrid(columns: Int) = SubGridSpec(
    columns = GridCells.Fixed(columns),
    contentPadding = PaddingValues(bottom = 8.dp),
    spacing = 16.dp,
    tv = false,
)

/**
 * 平板「我的」（原型 meTablet）：列表-详情。
 * 左列 width 340，padding(24, 12)，spacedBy 18：资料头 + 6 个入口（选中项 secondaryContainer，图标容器 primary / onPrimary）；
 * 右栏 margin(16, 20, 16, 4)，圆角 28，surfaceContainerLow，padding 24，Column(spacedBy 16)：随左侧选择原地切换内容。
 */
@Composable
internal fun MeTablet(nav: NavController, state: MeState) {
    var pane by rememberSaveable { mutableStateOf(MeEntry.History) }
    var historyFilter by rememberSaveable { mutableStateOf(HistoryFilter.All) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            Modifier
                .width(340.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MeHeader(state, onLogin = { nav.navigate(LoginRoute) })
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MeEntry.entries.forEach { entry ->
                    val selected = entry == pane
                    BvListItem(
                        title = stringResource(entry.title),
                        subtitle = state.subtitle(entry, tablet = true).ifEmpty { null },
                        icon = entry.icon,
                        iconContainerColor = if (selected) scheme.primary else scheme.surfaceContainerHigh,
                        iconContentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                        selected = selected,
                        onClick = { pane = entry },
                    )
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 16.dp, end = 20.dp, bottom = 16.dp + navBottom)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.surfaceContainerLow)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val needsLogin = pane != MeEntry.Accounts && !state.isLogin
            when {
                needsLogin -> {
                    PaneTitle(stringResource(pane.title))
                    SubPageLoginPrompt(
                        onLogin = { nav.navigate(LoginRoute) },
                        modifier = Modifier.weight(1f),
                        tv = false,
                    )
                }

                pane == MeEntry.History -> HistoryPane(
                    nav = nav,
                    state = state,
                    filter = historyFilter,
                    onFilter = { historyFilter = it },
                )

                pane == MeEntry.Favorite -> {
                    PaneTitle(stringResource(R.string.me_favorite))
                    FavoriteContent(
                        vm = state.favorite,
                        grid = paneGrid(4),
                        chipsPadding = PaddingValues(bottom = 12.dp),
                        onOpen = { nav.openVideo(it) },
                        modifier = Modifier.weight(1f),
                    )
                }

                pane == MeEntry.ToView -> {
                    PaneTitle(stringResource(R.string.me_toview))
                    ToViewContent(
                        vm = state.toView,
                        grid = paneGrid(4),
                        onOpen = { nav.openVideo(it) },
                        modifier = Modifier.weight(1f),
                    )
                }

                pane == MeEntry.Seasons -> {
                    val seasons: FollowingSeasonViewModel = koinViewModel(key = "me_seasons_${state.uid}")
                    PaneTitle(stringResource(R.string.me_seasons))
                    FollowingSeasonContent(
                        vm = seasons,
                        grid = paneGrid(6),
                        chipsPadding = PaddingValues(bottom = 12.dp),
                        onOpen = { nav.navigate(SeasonRoute(seasonId = it)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                pane == MeEntry.Following -> {
                    val follow: FollowViewModel = koinViewModel(key = "me_follow_${state.uid}")
                    PaneTitle(stringResource(R.string.me_following))
                    FollowingContent(
                        vm = follow,
                        grid = paneGrid(2).let { SubGridSpec(it.columns, it.contentPadding, 8.dp, false) },
                        onOpen = { nav.navigate(SpaceRoute(it.mid)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    PaneTitle(stringResource(R.string.me_accounts))
                    AccountsContent(
                        vm = state.accounts,
                        onAdd = { nav.navigate(LoginRoute) },
                        onUserLock = { nav.navigate(UserLockRoute) },
                        onSwitched = {},
                        contentPadding = PaddingValues(bottom = 8.dp),
                        modifier = Modifier.weight(1f),
                        tv = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaneTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = BvType.PaneTitle,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 右栏「历史记录」：Row(SpaceBetween)(标题 + BvChip 全部 / 视频 / 番剧) + 按日期分组（今天 / 昨天 / 更早）的
 * 4 列 VideoCard 网格（带进度）。快到底部时加载下一页。
 */
@Composable
private fun ColumnScope.HistoryPane(
    nav: NavController,
    state: MeState,
    filter: HistoryFilter,
    onFilter: (HistoryFilter) -> Unit,
) {
    val data = state.data
    val gridState = rememberLazyGridState()
    gridState.LoadMoreEffect { if (data.entries.isNotEmpty()) data.loadMore() }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaneTitle(stringResource(R.string.me_history), Modifier.weight(1f, fill = false))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryFilter.entries.forEach { item ->
                BvChip(text = stringResource(item.label), selected = item == filter, onClick = { onFilter(item) })
            }
        }
    }

    val entries = data.entries
    // 每次重组都按当前时间取「今天 0 点」，跨天后分组会立即更新；不能用列表长度做缓存 key。
    val today = startOfTodaySeconds()
    // 历史或筛选条件变化时重新分组；直接观察 SnapshotStateList，避免每次重组都主动拷贝。
    val groups by remember(today, filter) {
        derivedStateOf {
            val filtered = entries.filter { filter.accepts(it) }
            HistoryDay.entries.mapNotNull { day ->
                filtered.filter { it.day(today) == day }.takeIf { it.isNotEmpty() }?.let { day to it }
            }
        }
    }

    // 筛选「视频 / 番剧」后已加载的条目太少时继续翻页（网格项数不变时 LoadMoreEffect 不会再次触发），最多翻到 200 条
    val shown = groups.sumOf { it.second.size }
    LaunchedEffect(filter, entries.size, data.loading, data.noMore) {
        if (filter != HistoryFilter.All && shown < 12 && entries.size in 1..200 && !data.loading && !data.noMore) {
            data.loadMore()
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        if (groups.isEmpty()) {
            EmptyOrLoading(
                // 筛选后为空、还在自动翻页时按加载中显示
                loading = data.loading || (entries.size in 1..200 && !data.noMore),
                modifier = Modifier.fillMaxSize(),
                message = stringResource(R.string.history_empty),
                onRetry = data::reload,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                groups.forEach { (day, list) ->
                    item(key = "day_${day.name}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        SectionHeader(title = stringResource(day.label))
                    }
                    items(
                        count = list.size,
                        key = { index -> list[index].card.avid },
                        contentType = { "video" },
                    ) { index ->
                        val entry = list[index]
                        VideoCard(
                            data = entry.card,
                            onClick = { nav.openHistory(entry) },
                            progress = entry.progress,
                        )
                    }
                }
                if (!data.noMore) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                        LoadMoreIndicator()
                    }
                }
            }
        }
    }
}
