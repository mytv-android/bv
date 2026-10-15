package dev.aaa1115910.bv.ui.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.ActionButtonGroup
import dev.aaa1115910.bv.ui.designsystem.component.ActionItem
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvTabRow
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoRow
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import kotlinx.coroutines.launch

/**
 * 手机视频详情（原型 `detailPhone`）：状态栏高度的黑色区域 + 16:9 内嵌播放器，
 * 下方「简介 / 评论 N」标签 + 描边小标签「发弹幕」，标签内容用 HorizontalPager 切换。
 */
@Composable
internal fun VideoDetailPhone(
    ui: DetailUi,
    follow: DetailFollow?,
    actionItems: List<ActionItem>,
    currentCid: Long,
    commentVm: CommentViewModel,
    callbacks: DetailCallbacks,
    playerSlot: @Composable (Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val commentsLabel = if (ui.commentCount > 0) {
        stringResource(R.string.video_detail_tab_comments, context.formatCount(ui.commentCount))
    } else {
        stringResource(R.string.video_detail_tab_comments_plain)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )
        playerSlot(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Box(Modifier.fillMaxWidth()) {
            HorizontalDivider(
                Modifier.align(Alignment.BottomStart),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                BvTabRow(
                    tabs = listOf(stringResource(R.string.video_detail_tab_intro), commentsLabel),
                    selected = pagerState.currentPage,
                    onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                    modifier = Modifier.weight(1f),
                )
                BvChip(
                    text = stringResource(R.string.video_detail_send_danmaku),
                    selected = false,
                    onClick = callbacks.onSendDanmaku,
                    modifier = Modifier.padding(end = 16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    textStyle = BvType.Label,
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Top,
        ) { page ->
            if (page == 0) {
                PhoneIntro(
                    ui = ui,
                    follow = follow,
                    actionItems = actionItems,
                    currentCid = currentCid,
                    callbacks = callbacks,
                )
            } else {
                DetailCommentList(
                    commentVm = commentVm,
                    onShowPictures = callbacks.onShowPictures,
                    onOpenReplies = callbacks.onOpenReplies,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** 简介页：UP 主行、标题、信息行、操作按钮组、选集 / 合集、相关推荐（padding 14×16，间距 12）。 */
@Composable
private fun PhoneIntro(
    ui: DetailUi,
    follow: DetailFollow?,
    actionItems: List<ActionItem>,
    currentCid: Long,
    callbacks: DetailCallbacks,
) {
    val gutter = Modifier.padding(horizontal = 16.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "up") {
            DetailUpRow(
                ui = ui,
                follow = follow,
                onUpClick = callbacks.onUpClick,
                onFollowClick = callbacks.onFollowClick,
                modifier = gutter,
            )
        }
        item(key = "title") {
            DetailTitleBlock(
                ui = ui,
                titleStyle = BvType.Title,
                onTagClick = callbacks.onTagClick,
                modifier = gutter,
            )
        }
        if (ui.seasons.size > 1) {
            item(key = "seasons") { DetailSeasonChips(ui, callbacks.onSeasonSelect) }
        }
        if (ui.metaPhone.isNotEmpty()) {
            item(key = "meta") {
                Text(
                    ui.metaPhone,
                    modifier = gutter,
                    style = BvType.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item(key = "actions") { ActionButtonGroup(actionItems, gutter) }
        ui.rows.forEach { row ->
            item(key = "header-${row.key}") {
                SectionHeader(
                    title = row.title,
                    modifier = gutter,
                    action = row.action,
                    onAction = { callbacks.onOpenEpisodes(row) },
                )
            }
            item(key = "chips-${row.key}") {
                DetailEpisodeChips(
                    row = row,
                    currentCid = currentCid,
                    onPlay = { episode -> callbacks.onPlay(row, episode) },
                )
            }
        }
        if (ui.related.isNotEmpty()) {
            item(key = "related") {
                SectionHeader(stringResource(R.string.video_detail_related), gutter)
            }
            itemsIndexed(ui.related, key = { index, video -> "related-$index-${video.avid}" }) { _, video ->
                VideoRow(
                    data = video,
                    onClick = { callbacks.onRelatedClick(video) },
                    modifier = gutter,
                )
            }
        }
        item(key = "nav") { Box(Modifier.navigationBarsPadding()) }
    }
}
