package dev.aaa1115910.bv.ui.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvListItem
import dev.aaa1115910.bv.ui.designsystem.component.CardFrame
import dev.aaa1115910.bv.ui.designsystem.component.FullPageLoading
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PageMessage
import dev.aaa1115910.bv.ui.designsystem.component.PosterCard
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.component.VideoRow
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.TvRowPivot
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.ui.navigation.SpaceRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/** 综合结果里排在「影视」块前面的视频个数（平板 / 电视 4 列 × 2 行）。 */
private const val AllLeadingVideos = 8

/** 打开结果：视频 → 详情（openVideo），番剧 / 影视 → SeasonRoute，UP 主 → SpaceRoute。 */
internal class SearchOpeners(
    val video: (VideoCardData) -> Unit,
    val pgc: (PgcItem) -> Unit,
    val user: (UserItem) -> Unit,
    val aid: (Long) -> Unit,
)

@Composable
internal fun rememberSearchOpeners(nav: NavController, state: SearchUiState): SearchOpeners =
    remember(nav, state) {
        // 打开结果时把关键词记入历史（电视实时结果不经过「提交」）
        val record: () -> Unit = { if (state.submitted.isNotEmpty()) state.input.addSearchHistory(state.submitted) }
        SearchOpeners(
            video = { record(); nav.openVideo(it) },
            pgc = { record(); nav.navigate(SeasonRoute(seasonId = it.seasonId)) },
            user = { record(); nav.navigate(SpaceRoute(it.mid)) },
            aid = { record(); nav.openVideo(it) },
        )
    }

/** 分类标签行：综合 视频 番剧 影视 UP 主。电视上聚焦即选中。 */
@Composable
internal fun SearchCategoryChips(
    selected: SearchCategory,
    onSelect: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier,
    selectOnFocus: Boolean = false,
    selectedRequester: FocusRequester? = null,
    large: Boolean = false,
) {
    Row(
        modifier
            .focusRestorer()
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchCategory.entries.forEach { category ->
            val isSelected = category == selected
            BvChip(
                text = stringResource(category.label),
                selected = isSelected,
                onClick = { onSelect(category) },
                selectOnFocus = selectOnFocus,
                modifier = if (isSelected && selectedRequester != null) Modifier.focusRequester(selectedRequester) else Modifier,
                contentPadding = if (large) PaddingValues(horizontal = 22.dp, vertical = 12.dp) else PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                textStyle = if (large) BvType.Body.copy(fontWeight = FontWeight.SemiBold) else BvType.Small.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

/** 番剧 / 影视的 Meta：「番剧 · 9.6 分」。 */
@Composable
internal fun pgcMeta(item: PgcItem): String {
    val type = stringResource(if (item.bangumi) R.string.search_category_bangumi else R.string.search_category_ft)
    return if (item.rating != null) stringResource(R.string.search_pgc_meta_rating, type, item.rating) else type
}

/**
 * 番剧 / 影视横向卡（原型 searchTablet 影视块）：Row(padding 12, 圆角 20, surfaceContainerLow, spacedBy 14)：
 * PosterCard(96) + Column(spacedBy 6)(标题 Bold + Meta + 「▶ 立即观看」高 34)。
 */
@Composable
internal fun PgcWideCard(item: PgcItem, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PosterCard(
            title = item.title,
            cover = item.cover,
            onClick = onOpen,
            width = 96.dp,
            rating = item.rating,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.title,
                style = BvType.Body.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pgcMeta(item),
                style = BvType.Meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BvButton(
                text = stringResource(R.string.search_watch_now),
                onClick = onOpen,
                modifier = Modifier.padding(top = 8.dp),
                icon = Icons.Rounded.PlayArrow,
                height = 34.dp,
            )
        }
    }
}

/** 关注按钮（原型「+ 关注」）。未登录时不显示。 */
@Composable
internal fun FollowButton(mid: Long, follow: FollowState, modifier: Modifier = Modifier) {
    LaunchedEffect(mid) { follow.ensure(mid) }
    if (!follow.available) return
    val followed = follow.followed(mid)
    BvButton(
        text = stringResource(if (followed) R.string.search_following else R.string.search_follow),
        onClick = { follow.toggle(mid) },
        modifier = modifier,
        style = if (followed) BvButtonStyle.Tonal else BvButtonStyle.Filled,
        icon = if (followed) Icons.Rounded.Check else Icons.Rounded.Add,
    )
}

/** UP 主结果行：BvListItem（头像 + 名字 + 签名）+ 关注按钮。 */
@Composable
internal fun UserResultItem(
    user: UserItem,
    follow: FollowState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
) {
    BvListItem(
        title = user.name,
        modifier = modifier,
        subtitle = user.sign.ifBlank { null },
        onClick = onOpen,
        contentPadding = contentPadding,
        leading = { BvAvatar(url = user.avatar, size = 44.dp, fallbackText = user.name) },
        trailing = { FollowButton(user.mid, follow) },
    )
}

/**
 * 「综合」第一块：最匹配的 UP 主卡（原型 searchTablet）：Row(padding 16, 圆角 24, surfaceContainer, spacedBy 16)：
 * 头像 72 + Column(名字 16sp Bold + Meta) + 「+ 关注」+ 3 个最新视频封面（120 宽 16:10）。
 * 搜索接口只返回名字、头像、签名，所以 Meta 显示签名；最新视频用 UP 主空间接口取前 3 个。
 */
@Composable
internal fun UpHeroCard(
    user: UserItem,
    follow: FollowState,
    apiType: ApiType,
    onOpen: () -> Unit,
    onOpenVideo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val userRepository: UserRepository = koinInject()
    var covers by remember(user.mid) { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    LaunchedEffect(user.mid) {
        covers = withContext(Dispatchers.IO) {
            runCatching {
                userRepository.getSpaceVideos(mid = user.mid, preferApiType = apiType)
                    .videos.take(3).map { it.aid to it.cover }
            }.getOrDefault(emptyList())
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .bvFocus(scale = 1.04f, corner = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpen),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvAvatar(url = user.avatar, size = 72.dp, fallbackText = user.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    user.name,
                    style = BvType.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (user.sign.isNotBlank()) {
                    Text(
                        user.sign,
                        style = BvType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        FollowButton(user.mid, follow)
        if (covers.isNotEmpty()) {
            Row(
                Modifier
                    .focusRestorer()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                covers.forEach { (aid, cover) ->
                    CardFrame(
                        cover = cover,
                        aspectRatio = 16f / 10f,
                        onClick = { onOpenVideo(aid) },
                        width = 120.dp,
                    ) {}
                }
            }
        }
    }
}

/** 列表为空时：加载中显示整页加载，否则显示「没有找到相关结果」+ 重试。 */
@Composable
private fun EmptyResult(loading: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (loading) {
        FullPageLoading(modifier)
    } else {
        PageMessage(message = stringResource(R.string.search_no_result), modifier = modifier, onRetry = onRetry)
    }
}

// ---------- 平板 / 电视「完整结果」：网格 ----------

/**
 * 平板结果区与电视「完整结果」页共用的网格（原型 searchTablet）：
 * 综合 = 匹配的 UP 主卡 + 「视频」(排序) 4 列 + 「影视」横向卡行 + 其余视频；
 * 视频 = 4 列；番剧 / 影视 = 横向卡（电视为 6 列海报）；UP 主 = 列表项网格。
 */
@Composable
internal fun SearchResultsPane(
    state: SearchUiState,
    follow: FollowState,
    openers: SearchOpeners,
    tv: Boolean,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spacing: Dp = if (tv) 18.dp else 16.dp,
) {
    val category = state.category
    key(state.submitted, category) {
        when (category) {
            SearchCategory.All -> AllResultsGrid(state, follow, openers, tv, onFilter, modifier, contentPadding, spacing)
            SearchCategory.Video -> VideoResultsGrid(state, openers, tv, onFilter, modifier, contentPadding, spacing)
            SearchCategory.Bangumi, SearchCategory.Ft -> PgcResultsGrid(state, openers, tv, category, modifier, contentPadding, spacing)
            SearchCategory.User -> UserResultsGrid(state, follow, openers, tv, modifier, contentPadding, spacing)
        }
    }
}

/** 「视频」区块标题：右侧「按播放量 ›」，点击打开筛选。电视上顶部有大号「筛选」按钮，不再显示这个小入口。 */
@Composable
private fun VideoSectionHeader(
    state: SearchUiState,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
    showAction: Boolean = true,
) {
    SectionHeader(
        title = stringResource(R.string.search_section_video),
        modifier = modifier,
        action = if (showAction) stringResource(R.string.search_sort_action, stringResource(state.result.selectedOrder.labelRes())) else null,
        onAction = if (showAction) onFilter else null,
    )
}

@Composable
private fun gridModifier(modifier: Modifier, tv: Boolean): Modifier =
    if (tv) modifier.focusRestorer().focusGroup() else modifier

@Composable
private fun AllResultsGrid(
    state: SearchUiState,
    follow: FollowState,
    openers: SearchOpeners,
    tv: Boolean,
    onFilter: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
    spacing: Dp,
) {
    val loading = state.isLoading(SearchCategory.All)
    val videos = state.videoCards()
    val bangumi = state.bangumiItems()
    val ft = state.ftItems()
    val pgcs = remember(bangumi, ft) { bangumi + ft }
    val users = state.userItems()
    if (videos.isEmpty() && pgcs.isEmpty() && users.isEmpty()) {
        EmptyResult(loading, onRetry = { state.refresh() }, modifier)
        return
    }
    val gridState = rememberLazyGridState()
    gridState.OnNearEnd { if (videos.isNotEmpty()) state.loadMore(SearchType.Video) }
    val videoLoading = state.isLoading(SearchType.Video)
    TvColumnPivot {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = gridModifier(modifier, tv),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            users.firstOrNull()?.let { user ->
                item(key = "up", span = { GridItemSpan(maxLineSpan) }, contentType = "up") {
                    UpHeroCard(
                        user = user,
                        follow = follow,
                        apiType = state.apiType,
                        onOpen = { openers.user(user) },
                        onOpenVideo = openers.aid,
                    )
                }
            }
            if (videos.isNotEmpty()) {
                item(key = "videoHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    VideoSectionHeader(state, onFilter, showAction = !tv)
                }
            }
            val leading = videos.take(AllLeadingVideos)
            items(leading.size, key = { leading[it].avid }, contentType = { "video" }) { index ->
                val card = leading[index]
                VideoCard(data = card, onClick = { openers.video(card) }, showStats = true)
            }
            if (pgcs.isNotEmpty()) {
                item(key = "pgcHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    SectionHeader(title = stringResource(R.string.search_section_pgc))
                }
                item(key = "pgcRow", span = { GridItemSpan(maxLineSpan) }, contentType = "pgcRow") {
                    TvRowPivot {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRestorer()
                                .focusGroup(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(
                                items = pgcs,
                                key = { "${it.bangumi}-${it.seasonId}" },
                                contentType = { "pgc" },
                            ) { item ->
                                PgcWideCard(item, onOpen = { openers.pgc(item) }, modifier = Modifier.width(420.dp))
                            }
                        }
                    }
                }
            }
            val rest = videos.drop(AllLeadingVideos)
            items(rest.size, key = { rest[it].avid }, contentType = { "video" }) { index ->
                val card = rest[index]
                VideoCard(data = card, onClick = { openers.video(card) }, showStats = true)
            }
            if (videoLoading) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}

@Composable
private fun VideoResultsGrid(
    state: SearchUiState,
    openers: SearchOpeners,
    tv: Boolean,
    onFilter: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
    spacing: Dp,
) {
    val loading = state.isLoading(SearchType.Video)
    val videos = state.videoCards()
    if (videos.isEmpty()) {
        Column(modifier) {
            VideoSectionHeader(state, onFilter, Modifier.padding(contentPadding), showAction = !tv)
            EmptyResult(loading, onRetry = { state.refresh() }, Modifier.weight(1f))
        }
        return
    }
    val gridState = rememberLazyGridState()
    gridState.OnNearEnd { state.loadMore(SearchType.Video) }
    TvColumnPivot {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = gridModifier(modifier, tv),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            item(key = "videoHeader", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                VideoSectionHeader(state, onFilter, showAction = !tv)
            }
            items(videos.size, key = { videos[it].avid }, contentType = { "video" }) { index ->
                val card = videos[index]
                VideoCard(data = card, onClick = { openers.video(card) }, showStats = true)
            }
            if (loading) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}

@Composable
private fun PgcResultsGrid(
    state: SearchUiState,
    openers: SearchOpeners,
    tv: Boolean,
    category: SearchCategory,
    modifier: Modifier,
    contentPadding: PaddingValues,
    spacing: Dp,
) {
    val type = if (category == SearchCategory.Bangumi) SearchType.MediaBangumi else SearchType.MediaFt
    val loading = state.isLoading(type)
    val pgcList = if (category == SearchCategory.Bangumi) state.bangumiItems() else state.ftItems()
    if (pgcList.isEmpty()) {
        EmptyResult(loading, onRetry = { state.refresh() }, modifier)
        return
    }
    val gridState = rememberLazyGridState()
    gridState.OnNearEnd { state.loadMore(type) }
    TvColumnPivot {
        LazyVerticalGrid(
            // 电视：6 列海报（第 9.11 节模板）；平板：横向卡
            columns = if (tv) GridCells.Fixed(6) else GridCells.Adaptive(380.dp),
            state = gridState,
            modifier = gridModifier(modifier, tv),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(pgcList.size, key = { pgcList[it].seasonId }, contentType = { "pgc" }) { index ->
                val item = pgcList[index]
                if (tv) {
                    PosterCard(
                        title = item.title,
                        cover = item.cover,
                        onClick = { openers.pgc(item) },
                        rating = item.rating,
                        subtitle = pgcMeta(item),
                    )
                } else {
                    PgcWideCard(item, onOpen = { openers.pgc(item) }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (loading) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}

@Composable
private fun UserResultsGrid(
    state: SearchUiState,
    follow: FollowState,
    openers: SearchOpeners,
    tv: Boolean,
    modifier: Modifier,
    contentPadding: PaddingValues,
    spacing: Dp,
) {
    val loading = state.isLoading(SearchType.BiliUser)
    val users = state.userItems()
    if (users.isEmpty()) {
        EmptyResult(loading, onRetry = { state.refresh() }, modifier)
        return
    }
    val gridState = rememberLazyGridState()
    gridState.OnNearEnd { state.loadMore(SearchType.BiliUser) }
    TvColumnPivot {
        LazyVerticalGrid(
            columns = if (tv) GridCells.Fixed(3) else GridCells.Adaptive(340.dp),
            state = gridState,
            modifier = gridModifier(modifier, tv),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing / 2),
        ) {
            items(users.size, key = { users[it].mid }, contentType = { "user" }) { index ->
                val user = users[index]
                UserResultItem(user, follow, onOpen = { openers.user(user) })
            }
            if (loading) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}

// ---------- 手机：结果列表 ----------

/**
 * 手机结果（原型 searchPhone 提交后）：视频 = 相关视频行样式（VideoRow）；番剧 / 影视 = 横向卡；
 * UP 主 = BvListItem + 关注按钮。综合 = 匹配的 UP 主 + 前 3 个番剧 / 影视 + 视频。
 */
@Composable
internal fun SearchResultsList(
    state: SearchUiState,
    follow: FollowState,
    openers: SearchOpeners,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val category = state.category
    val loading = state.isLoading(category)
    val videos = state.videoCards()
    val bangumi = state.bangumiItems()
    val ft = state.ftItems()
    val users = state.userItems()
    val showVideos = category == SearchCategory.All || category == SearchCategory.Video
    val pgcs: List<PgcItem> = when (category) {
        SearchCategory.All -> remember(bangumi, ft) { (bangumi + ft).take(3) }
        SearchCategory.Bangumi -> bangumi
        SearchCategory.Ft -> ft
        else -> emptyList()
    }
    val shownUsers: List<UserItem> = when (category) {
        SearchCategory.All -> users.take(1)
        SearchCategory.User -> users
        else -> emptyList()
    }
    val empty = pgcs.isEmpty() && shownUsers.isEmpty() && (!showVideos || videos.isEmpty())
    if (empty) {
        // 手机顶栏盖在列表上，空状态也要避开 contentPadding 的顶部
        EmptyResult(loading, onRetry = { state.refresh() }, modifier.padding(contentPadding))
        return
    }
    val userItemPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    key(state.submitted, category) {
        val listState = rememberLazyListState()
        listState.OnNearEnd { state.loadMore(category) }
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(shownUsers, key = { "u${it.mid}" }, contentType = { "user" }) { user ->
                UserResultItem(user, follow, onOpen = { openers.user(user) }, contentPadding = userItemPadding)
            }
            items(pgcs, key = { "p${it.bangumi}-${it.seasonId}" }, contentType = { "pgc" }) { item ->
                PgcWideCard(item, onOpen = { openers.pgc(item) }, modifier = Modifier.fillMaxWidth())
            }
            if (showVideos) {
                items(videos.size, key = { videos[it].avid }, contentType = { "video" }) { index ->
                    val card = videos[index]
                    VideoRow(data = card, onClick = { openers.video(card) })
                }
            }
            if (loading) {
                item(contentType = "loading") { LoadMoreIndicator() }
            }
        }
    }
}
