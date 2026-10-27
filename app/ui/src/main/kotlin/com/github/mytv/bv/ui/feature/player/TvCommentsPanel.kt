package com.github.mytv.bv.ui.feature.player

import android.view.KeyEvent as AndroidKeyEvent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.BvChip
import com.github.mytv.bv.ui.designsystem.component.CommentItem
import com.github.mytv.bv.ui.designsystem.component.CommentQrCodeDialog
import com.github.mytv.bv.ui.designsystem.component.LoadMoreIndicator
import com.github.mytv.bv.ui.designsystem.component.commentQrLink
import com.github.mytv.bv.ui.designsystem.focus.TvColumnPivot
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.viewmodel.CommentViewModel
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
    val longPressGuard = remember { CommentLongPressGuard() }
    var qrLink by remember { mutableStateOf<String?>(null) }

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
                    items(
                        items = commentVm.comments,
                        key = { it.rpid },
                        contentType = { "comment" },
                    ) { comment ->
                        val qrLinkOfComment = remember(comment.rpid, comment.content) {
                            commentQrLink(comment.content)
                        }
                        CommentItem(
                            comment = comment,
                            // clickable 让遥控器确认键展开 / 收起长评论（短评论不溢出，切换无变化）
                            // onPreviewKeyEvent 捕获长按确认键：带链接的评论弹出二维码，无链接时保持原行为
                            modifier = Modifier
                                .bvFocus(scale = 1f, corner = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .onPreviewKeyEvent { event ->
                                    longPressGuard.handle(event, qrLinkOfComment) { qrLink = it }
                                }
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
    qrLink?.let { link ->
        CommentQrCodeDialog(
            link = link,
            onDismiss = {
                qrLink = null
                longPressGuard.reset()
            },
        )
    }
}

/**
 * 电视评论卡片长按确认键：带链接的评论弹出二维码；不带链接时不拦截，
 * 确认键仍按 clickable 原有逻辑展开 / 收起长评论。触发后吞掉 KeyUp，
 * 避免这次长按被 clickable 再当成一次点击。
 */
private class CommentLongPressGuard {
    private var swallowCenterUp = false

    /** 关闭二维码时重置，避免 KeyUp 被对话框窗口接走后留下「一直吞确认键」的状态。 */
    fun reset() {
        swallowCenterUp = false
    }

    fun handle(event: KeyEvent, qrLink: String?, onShowQrCode: (String) -> Unit): Boolean {
        val native = event.nativeKeyEvent
        val code = native.keyCode
        val centerKey = code == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
            code == AndroidKeyEvent.KEYCODE_ENTER ||
            code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
        val longPressKey = code == LONG_PRESS_KEY_CODE

        if (event.type == KeyEventType.KeyUp) {
            if ((centerKey || longPressKey) && swallowCenterUp) {
                swallowCenterUp = false
                return true
            }
            return false
        }
        if (event.type != KeyEventType.KeyDown) return false

        val isLongPress = longPressKey || (centerKey && native.isLongPress)
        if (!isLongPress || qrLink == null) return false

        swallowCenterUp = true
        onShowQrCode(qrLink)
        return true
    }

    private companion object {
        /** 部分盒子的「长按确认」键码（与 DeleteMode 保持一致） */
        const val LONG_PRESS_KEY_CODE = 763
    }
}
