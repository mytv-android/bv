package com.github.mytv.bv.ui.feature.dynamic

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.LoadMoreIndicator
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.home.EmptyOrLoading
import com.github.mytv.bv.ui.feature.home.LoadMoreEffect

/**
 * 平板动态（原型 dynTablet）：列表-详情。左列 width 470（标题 PageTitleLarge + 头像行 + DynamicCard，选中项 secondaryContainer）；
 * 右栏 weight 1，margin(16, 20, 16, 12)，圆角 28 surfaceContainerLow，内容 = 动态详情（DynamicDetailPane）。
 */
@Composable
internal fun DynamicTablet(state: DynamicPageState, actions: DynamicActions) {
    val listState = rememberLazyListState()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    listState.LoadMoreEffect { if (state.items.isNotEmpty()) state.loadMore() }

    // 默认选中第一条；筛选后原选中项不在列表里时改选第一条
    val first = state.items.firstOrNull()
    LaunchedEffect(first) {
        if (first != null && state.items.none { state.isSelected(it) }) state.select(first)
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            Modifier
                .width(470.dp)
                .fillMaxHeight()
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.dynamic_title),
                modifier = Modifier.padding(start = 16.dp),
                style = BvType.PageTitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!state.isLogin) {
                DynamicLoginPrompt(
                    onLogin = actions.onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                DynamicUpRow(
                    ups = state.ups,
                    selected = state.upFilter,
                    onSelect = state::selectUp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                )
                PullToRefreshBox(
                    isRefreshing = state.pulling,
                    onRefresh = state::refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (state.items.isEmpty()) {
                        EmptyOrLoading(
                            loading = state.loading || state.pulling,
                            modifier = Modifier.fillMaxSize(),
                            message = stringResource(R.string.dynamic_empty),
                            onRetry = state::loadMore,
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = navBottom + 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(
                                state.items,
                                key = { item -> item.id?.takeIf { it.isNotBlank() } ?: item.commentId },
                                contentType = { "dynamic" },
                            ) { item ->
                                DynamicCard(
                                    item = item,
                                    onClick = { state.select(item) },
                                    onOpenOrig = actions.open,
                                    onPictures = actions.onPictures,
                                    selected = state.isSelected(item),
                                )
                            }
                            if (state.loading) {
                                item(key = "loading", contentType = "loading") { LoadMoreIndicator() }
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 12.dp, top = 16.dp, end = 20.dp, bottom = 16.dp + navBottom)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            val selected = state.selected
            if (selected != null && state.isLogin) {
                DynamicDetailPane(
                    item = selected,
                    commentVm = state.commentVm,
                    onOpen = actions.open,
                    onPictures = actions.onPictures,
                    contentPadding = PaddingValues(22.dp),
                )
            } else {
                Text(
                    stringResource(R.string.dynamic_select_hint),
                    style = BvType.Small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
