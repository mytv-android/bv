package com.github.mytv.bv.ui.feature.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.ShortsItem
import com.github.mytv.bv.ui.designsystem.component.Ambient
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvIconButton
import com.github.mytv.bv.ui.designsystem.component.BvIconButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvTabRow
import com.github.mytv.bv.ui.designsystem.component.CinemaScrim
import com.github.mytv.bv.ui.designsystem.component.VideoRow
import com.github.mytv.bv.ui.designsystem.icon.BvIcons
import com.github.mytv.bv.ui.designsystem.theme.BvTheme
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.designsystem.theme.CoverTheme
import com.github.mytv.bv.ui.designsystem.theme.LocalThemePrefs
import com.github.mytv.bv.viewmodel.ShortsViewModel
import kotlinx.coroutines.launch

/**
 * 平板刷刷（原型 shortsTablet）：导航轨由外壳提供；中间视频区（强制深色）+ 右侧 380 信息栏（SupportingPane）。
 * 视频框居中，上下各 20，宽 = 高 × 9/16，圆角 28；右侧竖向浮动工具栏放操作，左侧放上一条 / 下一条。
 */
@Composable
internal fun ShortsTablet(
    vm: ShortsViewModel,
    pool: ShortsPlayerPool,
    comments: ShortsComments,
    actions: ShortsActions,
) {
    val items = vm.items
    val pagerState = rememberPagerState(
        initialPage = vm.currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    ) { items.size }
    PagerSettleEffect(pagerState, vm, pool)
    val current = items.getOrNull(pagerState.currentPage)
    LoadCommentsEffect(current?.aid, comments)

    Row(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            BvTheme(forceDark = true) {
                TabletVideoArea(vm, pool, actions, pagerState, current)
            }
        }
        if (current != null) {
            // 右侧栏跟随全局明暗，颜色取当前视频封面
            CoverTheme(current.cover) {
                TabletInfoPanel(
                    item = current,
                    vm = vm,
                    comments = comments,
                    actions = actions,
                    pagerState = pagerState,
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun TabletVideoArea(
    vm: ShortsViewModel,
    pool: ShortsPlayerPool,
    actions: ShortsActions,
    pagerState: PagerState,
    current: ShortsItem?,
) {
    val items = vm.items
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val pageSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Ambient(current?.cover)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))
        ) {
            val frameHeight = (maxHeight - 40.dp).coerceAtLeast(0.dp)
            val frameWidth = frameHeight * 9f / 16f
            val centerX = maxWidth / 2

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> items.getOrNull(page)?.aid ?: page.toLong() },
            ) { page ->
                val item = items.getOrNull(page) ?: return@VerticalPager
                CoverTheme(item.cover, forceDark = true) {
                    Box(Modifier.fillMaxSize()) {
                        TabletFrame(
                            item = item,
                            pool = pool,
                            actions = actions,
                            reduceMotion = reduceMotion,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(frameWidth, frameHeight),
                        )
                    }
                }
            }

            // 右侧：点赞 / 投币 / 收藏 / 分享（视频框右侧 18、底部 40）
            if (current != null) {
                CoverTheme(current.cover, forceDark = true) {
                    val state = vm.actionStateOf(current.aid)
                    VerticalFloatingToolbar(
                        expanded = true,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = centerX + frameWidth / 2 + 18.dp, bottom = 40.dp),
                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        contentPadding = PaddingValues(6.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BvIconButton(
                                icon = Icons.Filled.ThumbUp,
                                contentDescription = stringResource(R.string.shorts_like),
                                onClick = { actions.like(current) },
                                style = if (state.liked) BvIconButtonStyle.Filled else BvIconButtonStyle.Standard,
                            )
                            BvIconButton(
                                icon = BvIcons.Coin,
                                contentDescription = stringResource(R.string.shorts_coin),
                                onClick = { actions.coin(current) },
                                style = if (state.coinCount > 0) BvIconButtonStyle.Filled else BvIconButtonStyle.Standard,
                            )
                            BvIconButton(
                                icon = Icons.Filled.Star,
                                contentDescription = stringResource(R.string.shorts_favorite),
                                onClick = { actions.favorite(current) },
                                style = if (state.favoured) BvIconButtonStyle.Filled else BvIconButtonStyle.Standard,
                            )
                            BvIconButton(
                                icon = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.shorts_share),
                                onClick = { actions.share(current) },
                            )
                        }
                    }
                }
            }

            // 左侧：上一条 / 下一条（视频框左侧 24、垂直居中）
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width((centerX - frameWidth / 2 - 24.dp).coerceAtLeast(0.dp)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                VerticalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    contentPadding = PaddingValues(6.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val page = pagerState.currentPage
                        BvIconButton(
                            icon = Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.shorts_prev),
                            onClick = { scope.launch { pagerState.animateScrollToPage(page - 1, animationSpec = pageSpec) } },
                            enabled = page > 0,
                        )
                        BvIconButton(
                            icon = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.shorts_next),
                            onClick = { scope.launch { pagerState.animateScrollToPage(page + 1, animationSpec = pageSpec) } },
                            enabled = page < items.lastIndex,
                        )
                    }
                }
            }

            SpeedBoostPill(
                visible = pool.boosting,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 36.dp),
            )
        }
    }
}

/** 视频框：视频 + CinemaScrim + 底部「@UP」、标题、标签、进度 */
@Composable
private fun TabletFrame(
    item: ShortsItem,
    pool: ShortsPlayerPool,
    actions: ShortsActions,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val hearts = remember(item.aid) { HeartPops() }
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black)
            .shortsGestures(item, pool, actions, hearts, reduceMotion)
    ) {
        if (!item.isPortrait) Ambient(item.cover)
        ShortsVideo(pool, item, Modifier.fillMaxSize(), crop = item.isPortrait)
        CinemaScrim()
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.shorts_at_up, item.author.name),
                modifier = Modifier.clickable { actions.openSpace(item.author.mid) },
                style = BvType.Body.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.title,
                modifier = Modifier.clickable { actions.openVideo(item) },
                style = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val tags = item.tagsText()
            if (tags.isNotEmpty()) {
                Text(
                    tags,
                    style = BvType.Label.copy(fontWeight = FontWeight.Normal),
                    color = Color.White.copy(alpha = .85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ShortsProgress(pool = pool, item = item, modifier = Modifier.fillMaxWidth())
        }
        HeartLayer(hearts)
    }
}

/** 右侧信息栏：UP 主 + 关注、标题、Meta、评论 / 相关 */
@Composable
private fun TabletInfoPanel(
    item: ShortsItem,
    vm: ShortsViewModel,
    comments: ShortsComments,
    actions: ShortsActions,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val pageSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
            .padding(vertical = 24.dp, horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvAvatar(
                url = item.author.face,
                size = 44.dp,
                fallbackText = item.author.name,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { actions.openSpace(item.author.mid) },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.author.name,
                    style = BvType.Body.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.authorFans?.let { fans ->
                    Text(
                        stringResource(R.string.shorts_fans, shortsCount(fans)),
                        style = BvType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (item.author.mid > 0) {
                val following = vm.isFollowing(item.author.mid) == true
                BvButton(
                    text = stringResource(if (following) R.string.shorts_followed else R.string.shorts_follow),
                    onClick = { actions.follow(item) },
                    style = if (following) BvButtonStyle.Tonal else BvButtonStyle.Filled,
                    icon = if (following) Icons.Filled.Check else Icons.Filled.Add,
                    height = 36.dp,
                    horizontalPadding = 16.dp,
                )
            }
        }
        Text(
            item.title,
            modifier = Modifier.clickable { actions.openVideo(item) },
            style = TextStyle(fontSize = 17.sp, lineHeight = 23.8.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        val meta = listOfNotNull(
            item.likeCount?.let { stringResource(R.string.shorts_like_count, shortsCount(it)) },
            item.replyCount?.let { stringResource(R.string.shorts_reply_count, shortsCount(it)) },
            item.pubdate?.let { shortsPubdate(it) },
        ).joinToString(stringResource(R.string.shorts_meta_separator))
        if (meta.isNotEmpty()) {
            Text(meta, style = BvType.Meta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        val commentTab = item.replyCount?.let { stringResource(R.string.shorts_tab_comments, shortsCount(it)) }
            ?: stringResource(R.string.shorts_comment)
        BvTabRow(
            tabs = listOf(commentTab, stringResource(R.string.shorts_tab_related)),
            selected = tab,
            onSelect = { tab = it },
            horizontalPadding = 0.dp,
        )
        if (tab == 0) {
            ShortsCommentList(
                item = item,
                comments = comments,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                onOpenSpace = actions::openSpace,
            )
        } else {
            // 「相关」：刷刷队列里接下来的视频，点击跳到那一条
            val items = vm.items
            val base = pagerState.currentPage + 1
            val upcoming = items.drop(base).take(20)
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = upcoming,
                    key = { _, entry -> entry.aid },
                    contentType = { _, _ -> "video" },
                ) { index, next ->
                    VideoRow(
                        data = next.toCardData(),
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(base + index, animationSpec = pageSpec) }
                        },
                    )
                }
            }
        }
    }
}
