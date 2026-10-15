package dev.aaa1115910.bv.ui.feature.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.navigation.LocalBottomBarPadding

/**
 * 手机动态（原型 dynPhone）：「动态」(top = statusBar + 6, start 20, bottom 10) → UP 主头像行
 * (水平 16 下 12, spacedBy 14) → LazyColumn(水平 12, spacedBy 12) DynamicCard。
 */
@Composable
internal fun DynamicPhone(state: DynamicPageState, actions: DynamicActions) {
    val listState = rememberLazyListState()
    val bottom = LocalBottomBarPadding.current + 12.dp
    listState.LoadMoreEffect { if (state.items.isNotEmpty()) state.loadMore() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            stringResource(R.string.dynamic_title),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, top = 6.dp, bottom = 10.dp),
            style = BvType.PageTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!state.isLogin) {
            DynamicLoginPrompt(
                onLogin = actions.onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = bottom),
            )
        } else {
            // 外圈 2dp 画在头像外面，上下各留 4dp 不被裁掉（4 + 8 = 原型的下边距 12）
            DynamicUpRow(
                ups = state.ups,
                selected = state.upFilter,
                onSelect = state::selectUp,
                modifier = Modifier.padding(bottom = 8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
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
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottom),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, contentType = { "dynamic" }) { item ->
                            DynamicCard(
                                item = item,
                                onClick = { actions.open(item) },
                                onOpenOrig = actions.open,
                                onPictures = actions.onPictures,
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
}
