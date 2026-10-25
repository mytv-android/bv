package dev.aaa1115910.bv.ui.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomItem
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.CardFrame
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect

internal const val LiveTabRecommend = 0
internal const val LiveTabFollowing = 1
internal const val LiveTabArea = 2
internal const val LiveTabCount = 3

@Composable
internal fun rememberLiveTabNames(): List<String> = listOf(
    stringResource(R.string.live_tab_recommend),
    stringResource(R.string.live_tab_following),
    stringResource(R.string.live_tab_area),
)

/** 直播间卡片：封面 16:9 + LIVE 角标 + 看过人数角标，下方标题、UP 名和子分区。 */
@Composable
internal fun LiveRoomCard(
    item: LiveRoomItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
) {
    val watched = item.watchedShow?.textSmall?.takeIf { it.isNotBlank() }
        ?: item.watchedShow?.textLarge?.takeIf { it.isNotBlank() }
        ?: item.online.takeIf { it > 0 }?.toString().orEmpty()
    CardFrame(
        cover = item.cover.ifBlank { item.userCover.ifBlank { item.keyframe } },
        aspectRatio = 16f / 9f,
        onClick = onClick,
        modifier = modifier,
        width = width,
        coverContent = {
            LiveBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
            if (watched.isNotEmpty()) {
                Text(
                    watched,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = .55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = BvType.Badge.copy(fontFeatureSettings = "tnum"),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        },
    ) {
        Text(
            item.title,
            style = BvType.CardTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                item.uname,
                modifier = Modifier.weight(1f, fill = false),
                style = BvType.Meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.areaV2Name.isNotEmpty()) {
                Text(
                    item.areaV2Name,
                    style = BvType.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** LIVE 角标：error 色底白字胶囊（主题里为红色系）。 */
@Composable
internal fun LiveBadge(modifier: Modifier = Modifier) {
    Text(
        "LIVE",
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = .9f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = BvType.Badge,
        color = Color.White,
    )
}

/** 网格里的直播间卡片。 */
internal fun LazyGridScope.liveCards(
    list: List<LiveRoomItem>,
    onOpen: (LiveRoomItem) -> Unit,
) {
    items(list.size, key = { index -> list[index].roomId }, contentType = { "live" }) { index ->
        val item = list[index]
        LiveRoomCard(item = item, onClick = { onOpen(item) })
    }
}

/**
 * 直播间网格（推荐 / 关注 / 分区共用）：触底加载更多，空列表时整页加载 / 重试。
 * [onLoadMore] 推荐无分页时传 {}；[showLoadMore] 控制是否在底部显示加载指示。
 */
@Composable
internal fun LiveRoomGridPage(
    items: List<LiveRoomItem>,
    loading: Boolean,
    columns: GridCells,
    onOpen: (LiveRoomItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spacing: Dp = 12.dp,
    showLoadMore: Boolean = true,
) {
    val gridState = rememberLazyGridState()
    gridState.LoadMoreEffect { if (items.isNotEmpty()) onLoadMore() }
    if (items.isEmpty()) {
        EmptyOrLoading(
            loading = loading,
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = contentPadding.calculateBottomPadding()),
            onRetry = onRetry,
        )
    } else {
        LazyVerticalGrid(
            columns = columns,
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            liveCards(items, onOpen)
            if (loading && showLoadMore) {
                item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                    LoadMoreIndicator()
                }
            }
        }
    }
}

/** 分区标签横排（电视上聚焦即选中，同 BvTabRow 的做法）。 */
@Composable
internal fun LiveChipRow(
    names: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val selectOnFocus = LocalDeviceProfile.current.isTv
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceAtMost((names.size - 1).coerceAtLeast(0)))
    // 外部切换分区时，把选中的标签滚到可见处
    LaunchedEffect(selected) {
        val info = listState.layoutInfo
        val target = info.visibleItemsInfo.firstOrNull { it.index == selected }
        val fullyVisible = target != null &&
            target.offset >= info.viewportStartOffset && target.offset + target.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem(selected.coerceAtMost((names.size - 1).coerceAtLeast(0)))
    }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            names,
            key = { _, name -> name },
            contentType = { _, _ -> "tab" },
        ) { index, name ->
            BvChip(
                text = name,
                selected = index == selected,
                onClick = { onSelect(index) },
                selectOnFocus = selectOnFocus,
            )
        }
    }
}
