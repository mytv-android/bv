package dev.aaa1115910.bv.ui.feature.dynamic

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.TvRowPivot
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.feature.timeline.tryRequestFocus
import kotlinx.coroutines.delay

/** 内容区顶边（原型 dynTv：内容 at top 170）。 */
private val TvDynamicContentTop = 170.dp

/**
 * 电视动态（原型 dynTv）：顶部 (96, 27, 48)：Row(「动态」+ 标签 全部 / 视频 / 图文（聚焦即选中））+ 头像行；
 * 内容 at (96, 170)：按日期分组（今天 / 昨天 / 更早），每组 SectionHeader + 一行 240 宽的动态卡；第一张卡初始焦点。
 * 只展示有封面的动态（视频、图文、直播、专栏、转发的原动态），评论在详情里看。
 */
@Composable
internal fun DynamicTv(state: DynamicPageState, actions: DynamicActions) {
    val loginRequester = remember { FocusRequester() }
    val firstCardRequester = remember { FocusRequester() }
    val chipsRequester = remember { FocusRequester() }
    var entryPending by remember { mutableStateOf(true) }

    val items = state.items
    val groups = remember(items) {
        val byDate = items.asSequence().filter { it.coverUrl() != null }.groupBy { it.dateGroup() }
        DynamicDateGroup.entries.mapNotNull { group -> byDate[group]?.let { group to it } }
    }

    // 初始焦点：第一张卡；未登录时「登录」；一直没有内容时标签
    LaunchedEffect(state.isLogin, groups.isNotEmpty(), state.loading) {
        if (!entryPending) return@LaunchedEffect
        when {
            !state.isLogin -> {
                withFrameNanos { }
                loginRequester.tryRequestFocus()
                entryPending = false
            }

            groups.isNotEmpty() -> {
                withFrameNanos { }
                if (!firstCardRequester.tryRequestFocus()) chipsRequester.tryRequestFocus()
                entryPending = false
            }

            !state.loading -> {
                delay(1_000L)
                chipsRequester.tryRequestFocus()
                entryPending = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 27.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 96.dp, end = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.dynamic_title),
                    style = BvType.PageTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.isLogin) {
                    Row(
                        Modifier
                            .focusRequester(chipsRequester)
                            .focusRestorer()
                            .focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DynamicKind.entries.forEach { kind ->
                            BvChip(
                                text = stringResource(kind.label),
                                selected = state.kind == kind,
                                onClick = { state.kind = kind },
                                selectOnFocus = true,
                            )
                        }
                    }
                }
            }
            if (state.isLogin) {
                DynamicUpRow(
                    ups = state.ups,
                    selected = state.upFilter,
                    onSelect = state::selectUp,
                    // 视口从导航轨右边缘（收起 76dp）开始，头像不会滑到侧栏下面
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 76.dp)
                        .focusRestorer()
                        .focusGroup(),
                    contentPadding = PaddingValues(start = 20.dp, end = 48.dp, top = 4.dp, bottom = 4.dp),
                )
            }
        }

        if (!state.isLogin) {
            DynamicLoginPrompt(
                onLogin = actions.onLogin,
                modifier = Modifier.padding(start = 96.dp),
                buttonRequester = loginRequester,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = TvDynamicContentTop)
            ) {
                if (groups.isEmpty()) {
                    EmptyOrLoading(
                        loading = state.loading,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 96.dp),
                        message = stringResource(R.string.dynamic_empty),
                        onRetry = state::loadMore,
                    )
                } else {
                    TvColumnPivot {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 27.dp),
                        ) {
                            groups.forEachIndexed { index, (group, list) ->
                                item(key = group.name, contentType = "row") {
                                    TvDynamicRow(
                                        title = stringResource(group.label),
                                        items = list,
                                        onOpen = actions.open,
                                        onLoadMore = state::loadMore,
                                        firstRequester = if (index == 0) firstCardRequester else null,
                                    )
                                }
                            }
                            if (state.loading) {
                                item(key = "loading", contentType = "loading") { LoadMoreIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 一组：SectionHeader + TvRowPivot { LazyRow(spacedBy 18, end 48) DynamicTvCard }。 */
@Composable
private fun TvDynamicRow(
    title: String,
    items: List<DynamicItem>,
    onOpen: (DynamicItem) -> Unit,
    onLoadMore: () -> Unit,
    firstRequester: FocusRequester?,
) {
    val rowState = rememberLazyListState()
    rowState.LoadMoreEffect { if (items.isNotEmpty()) onLoadMore() }
    Column {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(start = 96.dp, end = 48.dp),
        )
        TvRowPivot {
            LazyRow(
                state = rowState,
                // 视口从导航轨右边缘（收起 76dp）开始：往右翻时卡片不会滑到侧栏下面
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 76.dp)
                    .focusRestorer()
                    .focusGroup(),
                contentPadding = PaddingValues(start = 24.dp, end = 48.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                itemsIndexed(
                    items,
                    key = { _, item -> item.id?.takeIf { it.isNotBlank() } ?: item.commentId },
                    contentType = { _, _ -> "card" },
                ) { index, item ->
                    DynamicTvCard(
                        item = item,
                        onClick = { onOpen(item) },
                        modifier = if (index == 0 && firstRequester != null) Modifier.focusRequester(firstRequester) else Modifier,
                    )
                }
            }
        }
    }
}
