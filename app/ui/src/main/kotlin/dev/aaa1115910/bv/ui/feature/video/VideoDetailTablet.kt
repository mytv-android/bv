package dev.aaa1115910.bv.ui.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.ActionButtonGroup
import dev.aaa1115910.bv.ui.designsystem.component.ActionItem
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvTabRow
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoRow
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.viewmodel.CommentViewModel

/**
 * 平板视频详情（原型 `detailTablet`）：左栏 720 宽（播放器 720×405 圆角 28 + 标题 + UP 主行与操作按钮 + 分 P），
 * 右栏 surfaceContainerLow 圆角 28：「评论 N / 相关推荐」标签 + 最热 / 最新 + 列表。
 * 窗口不到 1196dp 宽时左栏按比例缩小，给右栏至少留 320dp。
 */
@Composable
internal fun VideoDetailTablet(
    ui: DetailUi,
    follow: DetailFollow?,
    actionItems: List<ActionItem>,
    currentCid: Long,
    commentVm: CommentViewModel,
    callbacks: DetailCallbacks,
    playerSlot: @Composable (Modifier) -> Unit,
) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val leftWidth = (screenWidth - 24.dp * 3 - 320.dp).coerceIn(480.dp, 720.dp)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val commentsLabel = if (ui.commentCount > 0) {
        stringResource(R.string.video_detail_tab_comments, context.formatCount(ui.commentCount))
    } else {
        stringResource(R.string.video_detail_tab_comments_plain)
    }
    val hasRelated = ui.related.isNotEmpty()

    Row(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 20.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            Modifier
                .width(leftWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BvIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.video_detail_back),
                    onClick = callbacks.onBack,
                )
                Text(
                    stringResource(R.string.video_detail_title),
                    style = TabletHeaderStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            playerSlot(
                Modifier
                    .width(leftWidth)
                    .height(leftWidth * 9f / 16f)
                    .clip(RoundedCornerShape(28.dp))
            )
            DetailTitleBlock(
                ui = ui,
                titleStyle = BvType.TitleLarge,
                onTagClick = callbacks.onTagClick,
            )
            if (ui.seasons.size > 1) {
                DetailSeasonChips(ui, callbacks.onSeasonSelect, contentPadding = PaddingValues(0.dp))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BvAvatar(
                        url = ui.upFace,
                        size = 44.dp,
                        fallbackText = ui.upName,
                        modifier = Modifier.clickable(onClick = callbacks.onUpClick),
                    )
                    Column(
                        Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            ui.upName,
                            style = BvType.Body.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (ui.metaTablet.isNotEmpty()) {
                            Text(
                                ui.metaTablet,
                                style = BvType.Meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (follow != null && follow.followed != null) {
                        DetailFollowButton(
                            follow = follow,
                            onClick = callbacks.onFollowClick,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                ActionButtonGroup(actionItems, Modifier.width(330.dp))
            }
            ui.rows.forEach { row ->
                if (ui.rows.size > 1 || row.action != null) {
                    SectionHeader(
                        title = row.title,
                        action = row.action,
                        onAction = { callbacks.onOpenEpisodes(row) },
                    )
                }
                DetailEpisodeChips(
                    row = row,
                    currentCid = currentCid,
                    onPlay = { episode -> callbacks.onPlay(row, episode) },
                    contentPadding = PaddingValues(0.dp),
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(vertical = 16.dp, horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val tabs = if (hasRelated) {
                listOf(commentsLabel, stringResource(R.string.video_detail_tab_related))
            } else {
                listOf(commentsLabel)
            }
            val selected = if (hasRelated) tab else 0
            BvTabRow(
                tabs = tabs,
                selected = selected,
                onSelect = { tab = it },
                horizontalPadding = 0.dp,
            )
            if (selected == 0) {
                CommentSortChips(commentVm)
                DetailCommentList(
                    commentVm = commentVm,
                    onShowPictures = callbacks.onShowPictures,
                    onOpenReplies = callbacks.onOpenReplies,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    showSortChips = false,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items = ui.related,
                        key = { _, video -> video.avid },
                        contentType = { _, _ -> "related" },
                    ) { _, video ->
                        VideoRow(data = video, onClick = { callbacks.onRelatedClick(video) })
                    }
                    item(key = "end") { Box(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
