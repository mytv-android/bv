package dev.aaa1115910.bv.ui.feature.pgcindex

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.FullPageLoading
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PosterCard
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.feature.timeline.SubPagePhone
import dev.aaa1115910.bv.ui.feature.timeline.SubPageTabletHeader
import dev.aaa1115910.bv.ui.feature.timeline.SubPageTvTitle
import dev.aaa1115910.bv.ui.feature.timeline.tryRequestFocus
import dev.aaa1115910.bv.ui.navigation.PgcIndexRoute
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.viewmodel.index.PgcIndexViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 索引页的加载调度（PgcIndexViewModel 的加载状态是私有的）：加载串行执行，筛选变化时等当前加载结束后再重新加载，
 * 不取消正在进行的请求（取消会让 PgcIndexViewModel 的 updating 停在 true）。全部在主线程。
 */
internal class PgcIndexHolder : ViewModel() {
    var loadedKey: List<Any>? = null
    var loading by mutableStateOf(false)
        private set
    var noMore by mutableStateOf(false)
        private set
    private var pendingReload = false

    fun reload(vm: PgcIndexViewModel) {
        pendingReload = true
        if (!loading) runReload(vm)
    }

    fun loadMore(vm: PgcIndexViewModel) {
        if (loading || noMore) return
        loading = true
        viewModelScope.launch { finish(vm) }
    }

    private fun runReload(vm: PgcIndexViewModel) {
        pendingReload = false
        vm.clearData()
        noMore = false
        loading = true
        viewModelScope.launch { finish(vm) }
    }

    private suspend fun finish(vm: PgcIndexViewModel) {
        vm.loadMore()
        // 加载失败且列表仍为空时也停在「无可用数据」（可重试 / 改筛选），不无限转圈
        noMore = vm.noMore || vm.indexResultItems.isEmpty()
        loading = false
        if (pendingReload) runReload(vm)
    }
}

private fun PgcIndexViewModel.filterKey(): List<Any> = listOf(
    pgcType, indexOrder, indexOrderType, seasonVersion, spokenLanguage, area, isFinish,
    copyright, seasonStatus, seasonMonth, producer, year, releaseDate, style,
)

/**
 * 番剧索引（第 9.11 节）：PgcIndexViewModel + 筛选对话框（旧电视 PgcIndexScreen / IndexFilter）。
 * 海报网格：手机 3 列、平板 Adaptive(120)、电视 Fixed(6)。route.type = PgcType 的序号。
 */
@Composable
fun PgcIndexScreen(nav: NavController, route: PgcIndexRoute) {
    val vm: PgcIndexViewModel = koinViewModel()
    val holder: PgcIndexHolder = viewModel { PgcIndexHolder() }
    val type = PgcType.entries.getOrElse(route.type) { PgcType.Anime }
    var showFilter by rememberSaveable { mutableStateOf(false) }

    // 同旧界面：进入时设置类型；之后任一筛选条件变化都清空并重新加载
    LaunchedEffect(vm, type) {
        if (holder.loadedKey == null || vm.pgcType != type) vm.changePgcType(type)
        snapshotFlow { vm.filterKey() }
            .distinctUntilChanged()
            .collect { key ->
                if (key != holder.loadedKey) {
                    holder.loadedKey = key
                    holder.reload(vm)
                }
            }
    }

    val context = LocalContext.current
    val title = stringResource(R.string.pgcindex_title, type.getDisplayName(context))
    val content = PgcIndexContent(
        items = vm.indexResultItems,
        loading = holder.loading,
        noMore = holder.noMore,
        onOpen = { nav.navigate(SeasonRoute(seasonId = it.seasonId)) },
        onLoadMore = { holder.loadMore(vm) },
        onOpenFilter = { showFilter = true },
        onRetry = { holder.reload(vm) },
    )
    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> SubPagePhone(
            title = title,
            onBack = { nav.popBackStack() },
            actions = {
                BvIconButton(
                    icon = Icons.Filled.Tune,
                    contentDescription = stringResource(R.string.pgcindex_filter),
                    onClick = { showFilter = true },
                )
            },
        ) { bottom ->
            PgcIndexGrid(
                content = content,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom),
                spacing = 12.dp,
            )
        }

        DeviceLayout.Tablet -> Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SubPageTabletHeader(title = title, onBack = { nav.popBackStack() }) {
                BvButton(
                    text = stringResource(R.string.pgcindex_filter),
                    onClick = { showFilter = true },
                    style = BvButtonStyle.Tonal,
                    icon = Icons.Filled.Tune,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PgcIndexGrid(
                    content = content,
                    columns = GridCells.Adaptive(120.dp),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                    ),
                    spacing = 16.dp,
                )
            }
        }

        DeviceLayout.Tv -> PgcIndexTv(title = title, content = content)
    }

    if (showFilter) {
        PgcIndexFilterDialog(vm = vm, onDismiss = { showFilter = false })
    }
}

/** 三端共享的网格数据与动作。 */
internal class PgcIndexContent(
    val items: List<PgcItem>,
    val loading: Boolean,
    val noMore: Boolean,
    val onOpen: (PgcItem) -> Unit,
    val onLoadMore: () -> Unit,
    val onOpenFilter: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
private fun PgcIndexGrid(
    content: PgcIndexContent,
    columns: GridCells,
    contentPadding: PaddingValues,
    spacing: Dp,
    modifier: Modifier = Modifier,
    firstRequester: FocusRequester? = null,
) {
    val gridState = rememberLazyGridState()
    gridState.LoadMoreEffect { if (content.items.isNotEmpty()) content.onLoadMore() }
    when {
        content.items.isEmpty() && (content.loading || !content.noMore) -> FullPageLoading(modifier)
        content.items.isEmpty() -> PgcIndexEmpty(content, modifier)
        else -> LazyVerticalGrid(
            columns = columns,
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(content.items.size, contentType = { "poster" }) { index ->
                val item = content.items[index]
                PosterCard(
                    data = SeasonCardData.fromPgcItem(item),
                    onClick = { content.onOpen(item) },
                    modifier = if (index == 0 && firstRequester != null) Modifier.focusRequester(firstRequester) else Modifier,
                    // 同旧电视版：长按打开筛选
                    onLongClick = content.onOpenFilter,
                )
            }
            if (!content.noMore) {
                item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                    LoadMoreIndicator()
                }
            }
        }
    }
}

@Composable
private fun PgcIndexEmpty(content: PgcIndexContent, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.pgcindex_empty),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BvButton(
                    text = stringResource(R.string.pgcindex_open_filter),
                    onClick = content.onOpenFilter,
                    style = BvButtonStyle.Tonal,
                )
                BvButton(
                    text = stringResource(R.string.ds_retry),
                    onClick = content.onRetry,
                    style = BvButtonStyle.Outlined,
                )
            }
        }
    }
}

/** 电视：标题 at (48, 27) + 右侧「筛选」，Fixed(6) 海报网格（focusRestorer），第一项初始焦点。 */
@Composable
private fun PgcIndexTv(title: String, content: PgcIndexContent) {
    val firstRequester = remember { FocusRequester() }
    val filterRequester = remember { FocusRequester() }
    var entryPending by remember { mutableStateOf(true) }
    val ready = content.items.isNotEmpty() || (content.noMore && !content.loading)

    LaunchedEffect(ready) {
        if (!entryPending || !ready) return@LaunchedEffect
        withFrameNanos { }
        if (!(content.items.isNotEmpty() && firstRequester.tryRequestFocus())) filterRequester.tryRequestFocus()
        entryPending = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SubPageTvTitle(title) {
            Spacer(Modifier.weight(1f))
            BvButton(
                text = stringResource(R.string.pgcindex_filter),
                onClick = content.onOpenFilter,
                modifier = Modifier.focusRequester(filterRequester),
                style = BvButtonStyle.Tonal,
                icon = Icons.Filled.Tune,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            TvColumnPivot {
                PgcIndexGrid(
                    content = content,
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 27.dp),
                    spacing = 18.dp,
                    modifier = Modifier
                        .focusRestorer()
                        .focusGroup(),
                    firstRequester = firstRequester,
                )
            }
        }
    }
}
