package dev.aaa1115910.bv.ui.feature.video

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.BvInputMode
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.CommentItem
import dev.aaa1115910.bv.ui.designsystem.component.CommentQrCodeDialog
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.commentQrLink
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.imageviewer.previewer.ImagePreviewer
import dev.aaa1115910.bv.ui.imageviewer.previewer.ImagePreviewerState
import dev.aaa1115910.bv.ui.imageviewer.previewer.VerticalDragType
import dev.aaa1115910.bv.ui.imageviewer.previewer.rememberPreviewerState
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import kotlinx.coroutines.launch

/** 最热 / 最新切换（BvChip）。 */
@Composable
internal fun CommentSortChips(commentVm: CommentViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BvChip(
            text = stringResource(R.string.video_detail_sort_hot),
            selected = commentVm.commentSort == CommentSort.Hot,
            onClick = {
                if (commentVm.commentSort != CommentSort.Hot) {
                    scope.launch { commentVm.switchCommentSort(CommentSort.Hot) }
                }
            },
        )
        BvChip(
            text = stringResource(R.string.video_detail_sort_time),
            selected = commentVm.commentSort == CommentSort.Time,
            onClick = {
                if (commentVm.commentSort != CommentSort.Time) {
                    scope.launch { commentVm.switchCommentSort(CommentSort.Time) }
                }
            },
        )
    }
}

/**
 * 评论列表（CommentViewModel）：顶部最热 / 最新标签，下拉刷新，快到底部时加载更多；
 * 有回复的评论点「回复 N」打开回复抽屉。
 */
@Composable
internal fun DetailCommentList(
    commentVm: CommentViewModel,
    onShowPictures: (List<Picture>, Int) -> Unit,
    onOpenReplies: (Comment) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    itemSpacing: Dp = 14.dp,
    showSortChips: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val touchInput = LocalDeviceProfile.current.input == BvInputMode.Touch
    var qrLink by remember { mutableStateOf<String?>(null) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last > listState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore, commentVm.comments.size, commentVm.commentId) {
        if (shouldLoadMore && commentVm.commentId != 0L && commentVm.hasMoreComments && !commentVm.updatingComments) {
            commentVm.loadMoreComment()
        }
    }
    PullToRefreshBox(
        isRefreshing = commentVm.refreshingComments,
        onRefresh = { scope.launch { commentVm.refreshComments() } },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            if (showSortChips) {
                item(key = "sort") { CommentSortChips(commentVm) }
            }
            items(commentVm.comments) { comment ->
                CommentItem(
                    comment = comment,
                    onPictureClick = onShowPictures,
                    onReplyClick = if (comment.repliesCount > 0) {
                        { onOpenReplies(comment) }
                    } else null,
                    onLongClick = if (touchInput) {
                        { commentQrLink(comment.content)?.let { qrLink = it } }
                    } else null,
                )
            }
            if (commentVm.updatingComments) {
                item(key = "loading") { LoadMoreIndicator() }
            } else if (commentVm.comments.isEmpty() && !commentVm.refreshingComments) {
                item(key = "empty") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.video_detail_comments_empty),
                            style = BvType.Small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item(key = "nav") { Box(Modifier.navigationBarsPadding()) }
        }
    }
    qrLink?.let { link ->
        CommentQrCodeDialog(link = link, onDismiss = { qrLink = null })
    }
}

/**
 * 回复抽屉：由旧手机 ReplySheetScaffold / Replies 改写（数据走 CommentViewModel 的 replies），
 * 根评论 + 「相关回复共 N 条」+ 排序切换 + 回复列表。
 */
@Composable
internal fun RepliesSheet(
    commentVm: CommentViewModel,
    onShowPictures: (List<Picture>, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val touchInput = LocalDeviceProfile.current.input == BvInputMode.Touch
    var qrLink by remember { mutableStateOf<String?>(null) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last > listState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore, commentVm.replies.size) {
        if (shouldLoadMore && commentVm.hasMoreReplies && !commentVm.updatingReplies && !commentVm.refreshingReplies) {
            commentVm.loadMoreReplies()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            commentVm.replyRootComment?.let { root ->
                item(key = "root") {
                    CommentItem(
                        comment = root,
                        maxLines = Int.MAX_VALUE,
                        onPictureClick = onShowPictures,
                        onLongClick = if (touchInput) {
                            { commentQrLink(root.content)?.let { qrLink = it } }
                        } else null,
                    )
                }
            }
            item(key = "header") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.video_detail_replies_title, commentVm.rpCount),
                        style = BvType.Section,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BvChip(
                            text = stringResource(R.string.video_detail_sort_hot),
                            selected = commentVm.replySort == CommentSort.Hot,
                            onClick = {
                                if (commentVm.replySort != CommentSort.Hot) {
                                    scope.launch { commentVm.switchReplySort(CommentSort.Hot) }
                                }
                            },
                        )
                        BvChip(
                            text = stringResource(R.string.video_detail_sort_time),
                            selected = commentVm.replySort == CommentSort.Time,
                            onClick = {
                                if (commentVm.replySort != CommentSort.Time) {
                                    scope.launch { commentVm.switchReplySort(CommentSort.Time) }
                                }
                            },
                        )
                    }
                }
            }
            items(commentVm.replies) { reply ->
                CommentItem(
                    comment = reply,
                    onPictureClick = onShowPictures,
                    onLongClick = if (touchInput) {
                        { commentQrLink(reply.content)?.let { qrLink = it } }
                    } else null,
                )
            }
            if (commentVm.updatingReplies || commentVm.refreshingReplies) {
                item(key = "loading") { LoadMoreIndicator() }
            }
            item(key = "nav") { Box(Modifier.navigationBarsPadding()) }
        }
    }
    qrLink?.let { link ->
        CommentQrCodeDialog(link = link, onDismiss = { qrLink = null })
    }
}

/**
 * 打开回复抽屉前同步重置回复状态（避免抽屉先按旧的 rpid 加载），之后调用 refreshReplies 加载。
 */
internal fun CommentViewModel.prepareReplies(comment: Comment) {
    rpid = comment.rpid
    rpCount = comment.repliesCount
    replyRootComment = comment
    replies.clear()
    hasMoreReplies = false
}

/** 评论图片预览（沿用旧手机的 ImagePreviewer）。 */
@Stable
internal class CommentPictures(
    val previewer: ImagePreviewerState,
    val pictures: SnapshotStateList<Picture>,
)

@Composable
internal fun rememberCommentPictures(): CommentPictures {
    val pictures = remember { mutableStateListOf<Picture>() }
    val previewer = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = { pictures.size },
        getKey = { index -> pictures.getOrNull(index)?.key ?: index },
    )
    return remember(previewer, pictures) { CommentPictures(previewer, pictures) }
}

/** 显示图片预览层，并处理返回键关闭。放在页面最上层。 */
@Composable
internal fun CommentPicturePreviewer(state: CommentPictures) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val previewer = state.previewer
    BackHandler(previewer.canClose || previewer.animating) {
        if (previewer.canClose) scope.launch { previewer.close() }
    }
    Box(Modifier.fillMaxSize()) {
        ImagePreviewer(
            modifier = Modifier.fillMaxSize(),
            state = previewer,
            imageLoader = { index ->
                val url = state.pictures.getOrNull(index)?.url
                val request = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(coil.size.Size.ORIGINAL)
                        .build()
                }
                rememberAsyncImagePainter(request)
            },
        )
    }
}

/** 打开图片预览（第 [index] 张）。 */
internal suspend fun CommentPictures.show(pictures: List<Picture>, index: Int) {
    this.pictures.clear()
    this.pictures.addAll(pictures)
    previewer.open(index)
}
