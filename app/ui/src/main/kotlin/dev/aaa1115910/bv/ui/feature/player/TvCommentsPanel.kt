package dev.aaa1115910.bv.ui.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.CommentItem
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import kotlinx.coroutines.launch

/**
 * 电视播放器的评论面板（控制栏「评论」按钮）：与菜单面板同样的右侧面板（top/end/bottom 16，圆角 28，
 * surfaceContainer），最热 / 最新切换 + 评论列表；方向键逐条移动，返回键关闭。
 */
@Composable
internal fun TvCommentsPanel(
    commentVm: CommentViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val firstRequester = remember { FocusRequester() }
    // 长评论展开状态：按 rpid 记录，切换排序 / 加载更多后保持
    val expandedComments = remember { mutableStateMapOf<Long, Boolean>() }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        runCatching { firstRequester.requestFocus() }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last > listState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore, commentVm.comments.size) {
        if (shouldLoadMore && commentVm.hasMoreComments && !commentVm.updatingComments) {
            commentVm.loadMoreComment()
        }
    }

    Box(
        modifier
            .fillMaxHeight()
            .padding(16.dp)
            .width(340.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 22.dp, start = 22.dp, end = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.player_screen_comments),
                style = BvType.HeroPhone,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                Modifier
                    .focusRestorer()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BvChip(
                    text = stringResource(R.string.player_screen_sort_hot),
                    selected = commentVm.commentSort == CommentSort.Hot,
                    onClick = {
                        if (commentVm.commentSort != CommentSort.Hot) {
                            scope.launch { commentVm.switchCommentSort(CommentSort.Hot) }
                        }
                    },
                    modifier = Modifier.focusRequester(firstRequester),
                )
                BvChip(
                    text = stringResource(R.string.player_screen_sort_time),
                    selected = commentVm.commentSort == CommentSort.Time,
                    onClick = {
                        if (commentVm.commentSort != CommentSort.Time) {
                            scope.launch { commentVm.switchCommentSort(CommentSort.Time) }
                        }
                    },
                )
            }
            TvColumnPivot {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(commentVm.comments) { comment ->
                        CommentItem(
                            comment = comment,
                            // clickable 让遥控器确认键展开 / 收起长评论（短评论不溢出，切换无变化）
                            modifier = Modifier
                                .bvFocus(scale = 1f, corner = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    expandedComments[comment.rpid] = expandedComments[comment.rpid] != true
                                }
                                .padding(8.dp),
                            maxLines = 8,
                            textExpanded = expandedComments[comment.rpid] == true,
                            onToggleTextExpand = {
                                expandedComments[comment.rpid] = expandedComments[comment.rpid] != true
                            },
                        )
                    }
                    if (commentVm.updatingComments) {
                        item(key = "loading") { LoadMoreIndicator() }
                    } else if (commentVm.comments.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                stringResource(R.string.player_screen_comments_empty),
                                modifier = Modifier.padding(8.dp),
                                style = BvType.Small,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
