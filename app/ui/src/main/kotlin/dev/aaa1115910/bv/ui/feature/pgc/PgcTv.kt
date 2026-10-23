package dev.aaa1115910.bv.ui.feature.pgc

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.CarouselData
import dev.aaa1115910.biliapi.entity.pgc.PgcItem
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PosterCard
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.TvBackdrop
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.TvRowPivot
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.designsystem.theme.CoverTheme
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import dev.aaa1115910.bv.ui.feature.timeline.tryRequestFocus
import dev.aaa1115910.bv.ui.navigation.FollowingSeasonRoute
import dev.aaa1115910.bv.ui.navigation.PgcIndexRoute
import dev.aaa1115910.bv.ui.navigation.PgcTimelineRoute
import kotlinx.coroutines.delay

/** 内容行区域的顶边（原型 pgcTv：行 at top 318）。 */
private val TvRowsTop = 318.dp

/**
 * 电视影视（原型 pgcTv）：整页 CoverTheme(当前精选封面, forceDark)，TvBackdrop；
 * 顶栏 (96, 24, 48) 标签（聚焦即选中）+ 时钟；信息区 (96, 78) width 420（「▶ 看第 N 话」为初始焦点）；
 * 行 at (96, 318)：「热门」+ 排行榜各一行 PosterCard(112)。
 */
@Composable
internal fun PgcTv(nav: NavController, state: PgcUiState) {
    val names = pgcTabNames()
    val feed = state.feed
    val featured = feed.featured
    val type = state.type
    val model = state.model
    val current = if (featured.isEmpty()) null else featured[model.featuredIndex(type) % featured.size]

    val playRequester = remember { FocusRequester() }
    val chipRequesters = remember { List(PgcTabs.size) { FocusRequester() } }
    var entryPending by remember { mutableStateOf(true) }
    var focusInInfo by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 精选每 8 秒切换到下一条；焦点在信息区时不切换
    LaunchedEffect(type, focusInInfo, featured.size) {
        if (focusInInfo || featured.size < 2) return@LaunchedEffect
        while (true) {
            delay(8_000L)
            model.setFeaturedIndex(type, (model.featuredIndex(type) + 1) % featured.size)
        }
    }

    // 初始焦点：「▶ 看第 N 话」；精选一直没有加载出来时聚焦当前标签
    LaunchedEffect(current != null) {
        if (!entryPending) return@LaunchedEffect
        if (current == null) {
            delay(1_500L)
            if (!entryPending) return@LaunchedEffect
            chipRequesters[state.tab].tryRequestFocus()
        } else {
            withFrameNanos { }
            if (!playRequester.tryRequestFocus()) chipRequesters[state.tab].tryRequestFocus()
        }
        entryPending = false
    }

    CoverTheme(current?.cover, forceDark = true) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            TvBackdrop(cover = current?.cover)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 96.dp, top = 24.dp, end = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .onFocusChanged { if (it.hasFocus) entryPending = false }
                        .focusRestorer()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    names.forEachIndexed { index, name ->
                        BvChip(
                            text = name,
                            selected = index == state.tab,
                            // 同旧电视版：在已选中的标签上按确认键重新加载该分类
                            onClick = { if (index == state.tab) feed.vm.reloadAll() else state.onTab(index) },
                            modifier = Modifier.focusRequester(chipRequesters[index]),
                            selectOnFocus = true,
                        )
                    }
                }
                PgcTvClock()
            }

            if (current != null) {
                TvInfoBlock(
                    item = current,
                    state = state,
                    nav = nav,
                    playRequester = playRequester,
                    modifier = Modifier
                        .padding(start = 96.dp, top = 78.dp)
                        .width(420.dp)
                        .onFocusChanged { focusInInfo = it.hasFocus },
                )
            }

            if (featured.isEmpty() && feed.hot.isEmpty() && feed.ranks.isEmpty()) {
                EmptyOrLoading(
                    loading = feed.vm.updating,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 96.dp, top = 74.dp),
                    message = stringResource(R.string.pgc_empty),
                    onRetry = { feed.vm.reloadAll() },
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = TvRowsTop)
                ) {
                    TvPgcRows(nav, state, names[state.tab], listState)
                }
            }
        }
    }
}

/**
 * 信息区 Column(spacedBy 10)：Eyebrow「本季热播 · 评分 x」、标题 Display 2 行、Meta 12sp「更新至第 N 话 · 每周五 23:00 · 风格」、
 * 按钮行（高 52）、标签行「时间表」「索引」「我的追番」。
 */
@Composable
private fun TvInfoBlock(
    item: CarouselData.CarouselItem,
    state: PgcUiState,
    nav: NavController,
    playRequester: FocusRequester,
    modifier: Modifier,
) {
    LaunchedEffect(item) { state.model.ensureExtra(item) }
    val extra = state.model.extraOf(item)
    val rating = state.feed.ratingOf(extra?.seasonId ?: item.seasonId)
    val followed = state.follow.isFollowed(item, extra)
    val meta = extra?.progressLine(withStyles = true).orEmpty()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (rating != null) stringResource(R.string.pgc_featured_rating_tv, rating)
            else stringResource(R.string.pgc_featured),
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
        if (meta.isNotEmpty()) {
            Text(
                meta,
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier
                .padding(top = 6.dp)
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BvButton(
                text = watchLabel(extra),
                onClick = { nav.openFeatured(item) },
                modifier = Modifier.focusRequester(playRequester),
                icon = Icons.Filled.PlayArrow,
                large = true,
            )
            BvButton(
                text = stringResource(if (followed) R.string.pgc_followed else R.string.pgc_follow),
                onClick = { state.follow.toggle(item, extra) },
                style = BvButtonStyle.Tonal,
                icon = if (followed) Icons.Filled.Check else Icons.Filled.Add,
                large = true,
            )
        }
        Row(
            Modifier
                .padding(top = 4.dp)
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.type.timelineFilter != null) {
                BvChip(
                    text = stringResource(R.string.pgc_timeline_chip),
                    selected = false,
                    onClick = { nav.navigate(PgcTimelineRoute) },
                )
            }
            BvChip(
                text = stringResource(R.string.pgc_index),
                selected = false,
                onClick = { nav.navigate(PgcIndexRoute(state.type.ordinal)) },
            )
            BvChip(
                text = stringResource(R.string.pgc_my_following),
                selected = false,
                onClick = { nav.navigate(FollowingSeasonRoute) },
            )
        }
    }
}

/** 行区域：TvColumnPivot { LazyColumn }，每行 SectionHeader + TvRowPivot { LazyRow(spacedBy 16, top 12, end 48) }。 */
@Composable
private fun TvPgcRows(nav: NavController, state: PgcUiState, tabName: String, listState: LazyListState) {
    val feed = state.feed
    TvColumnPivot {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 27.dp),
        ) {
            if (feed.hot.isNotEmpty()) {
                item(key = "hot", contentType = "row") {
                    TvPosterRow(
                        title = stringResource(R.string.pgc_hot, tabName),
                        posters = feed.hot,
                        onOpen = { nav.openSeason(it.seasonId) },
                        onLoadMore = feed::loadMore,
                    )
                }
            }
            itemsIndexed(feed.ranks, key = { _, rank -> "rank-${rank.title}" }, contentType = { _, _ -> "row" }) { _, rank ->
                TvPosterRow(
                    title = rank.title,
                    posters = rank.items,
                    onOpen = { nav.openSeason(it.seasonId) },
                )
            }
            if (feed.vm.updating) {
                item(key = "loading", contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}

@Composable
private fun TvPosterRow(
    title: String,
    posters: List<PgcItem>,
    onOpen: (PgcItem) -> Unit,
    onLoadMore: (() -> Unit)? = null,
) {
    val rowState = rememberLazyListState()
    if (onLoadMore != null) rowState.LoadMoreEffect { if (posters.isNotEmpty()) onLoadMore() }
    Column {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(start = 96.dp, end = 48.dp),
        )
        TvRowPivot {
            LazyRow(
                state = rowState,
                // 视口从导航轨右边缘（收起 76dp）开始：往右翻的时候榜单不会滑到侧边栏下面
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 76.dp)
                    .focusRestorer()
                    .focusGroup(),
                contentPadding = PaddingValues(start = 24.dp, end = 48.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(posters.size, key = { posters[it].seasonId }, contentType = { "poster" }) { index ->
                    val item = posters[index]
                    PosterCard(data = item.toCard(), onClick = { onOpen(item) }, width = 112.dp)
                }
            }
        }
    }
}
