package com.github.mytv.bv.ui.feature.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.BvTabRow
import com.github.mytv.bv.ui.designsystem.component.CommentItem
import com.github.mytv.bv.ui.designsystem.component.FullPageLoading
import com.github.mytv.bv.ui.designsystem.component.LoadMoreIndicator
import com.github.mytv.bv.ui.designsystem.component.PageMessage
import com.github.mytv.bv.ui.designsystem.focus.TvColumnPivot
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.home.LoadMoreEffect
import com.github.mytv.bv.ui.feature.timeline.SubPagePhone
import com.github.mytv.bv.ui.feature.timeline.SubPageTabletHeader
import com.github.mytv.bv.ui.feature.timeline.SubPageTvTitle
import com.github.mytv.bv.ui.feature.timeline.tryRequestFocus
import com.github.mytv.bv.ui.feature.video.CommentPicturePreviewer
import com.github.mytv.bv.ui.feature.video.RepliesSheet
import com.github.mytv.bv.ui.feature.video.prepareReplies
import com.github.mytv.bv.ui.feature.video.rememberCommentPictures
import com.github.mytv.bv.ui.feature.video.show
import com.github.mytv.bv.ui.navigation.DynamicDetailRoute
import com.github.mytv.bv.viewmodel.CommentViewModel
import com.github.mytv.bv.viewmodel.DynamicDetailViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** 动态详情页的加载状态（DynamicDetailViewModel 没有加载中 / 失败状态）。 */
internal class DynamicDetailHolder : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    fun load(vm: DynamicDetailViewModel, commentVm: CommentViewModel, id: String) {
        if (loading) return
        loading = true
        failed = false
        viewModelScope.launch {
            vm.dynamicId = id
            vm.loadDynamic()
            val item = vm.dynamicItem
            if (item == null) failed = true else commentVm.showCommentsOf(item)
            loading = false
        }
    }
}

/**
 * 动态详情（第 9.11 节）：DynamicDetailViewModel + CommentViewModel（旧手机 DynamicDetailScreen），
 * 布局 = 第 9.7 节平板右栏内容全屏化（手机 / 电视全屏；平板从别处打开时同样全屏）。
 */
@Composable
fun DynamicDetailScreen(nav: NavController, route: DynamicDetailRoute) {
    val vm: DynamicDetailViewModel = koinViewModel()
    val commentVm: CommentViewModel = koinViewModel()
    val holder: DynamicDetailHolder = viewModel { DynamicDetailHolder() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pictures = rememberCommentPictures()
    val layout = LocalDeviceProfile.current.layout
    val title = stringResource(R.string.dynamic_detail_title)
    val onBack: () -> Unit = { nav.popBackStack() }

    LaunchedEffect(route.id) {
        if (vm.dynamicId == route.id && vm.dynamicItem != null) return@LaunchedEffect
        holder.load(vm, commentVm, route.id)
    }

    val body: @Composable (PaddingValues, FocusRequester?) -> Unit = { padding, requester ->
        val item = vm.dynamicItem
        when {
            item != null -> DynamicDetailPane(
                item = item,
                commentVm = commentVm,
                onOpen = { openDynamic(nav, context, it) },
                onPictures = { list, index -> scope.launch { pictures.show(list, index) } },
                contentPadding = padding,
                initialFocus = requester,
            )

            holder.failed -> PageMessage(
                message = stringResource(R.string.dynamic_load_failed),
                onRetry = { holder.load(vm, commentVm, route.id) },
            )

            else -> FullPageLoading()
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (layout) {
            DeviceLayout.Phone -> SubPagePhone(title = title, onBack = onBack) { bottom ->
                body(PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottom), null)
            }

            DeviceLayout.Tablet -> Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                SubPageTabletHeader(title = title, onBack = onBack)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    body(PaddingValues(22.dp), null)
                }
            }

            DeviceLayout.Tv -> {
                val requester = remember { FocusRequester() }
                LaunchedEffect(vm.dynamicItem != null) {
                    if (vm.dynamicItem == null) return@LaunchedEffect
                    withFrameNanos { }
                    requester.tryRequestFocus()
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    SubPageTvTitle(title)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        body(PaddingValues(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 27.dp), requester)
                    }
                }
            }
        }
        CommentPicturePreviewer(pictures)
    }
}

/**
 * 动态详情内容（平板右栏，原型 dynTablet 右侧）：Column(spacedBy 14)：头部（头像 44 + 名字 / 时间）、
 * 媒体区（视频：封面 + 居中 64dp 播放按钮，圆角 24，高 300；其他类型：动态内容）、标题 16sp Bold、
 * BvTabRow「评论 N」「转发」+ CommentItem 列表。
 */
@Composable
internal fun DynamicDetailPane(
    item: DynamicItem,
    commentVm: CommentViewModel,
    onOpen: (DynamicItem) -> Unit,
    onPictures: (List<Picture>, Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    initialFocus: FocusRequester? = null,
) {
    val isTv = LocalDeviceProfile.current.isTv
    val listState = rememberLazyListState()
    var tab by rememberSaveable(item.id) { mutableIntStateOf(0) }
    var showReplies by remember { mutableStateOf(false) }
    listState.LoadMoreEffect { if (tab == 0) commentVm.loadMoreCommentsIfNeeded() }

    // 换了一条动态时回到顶部
    LaunchedEffect(item) { listState.scrollToItem(0) }

    val hasMedia = item.type == DynamicType.Av || item.type == DynamicType.Pgc || item.type == DynamicType.LiveRcmd
    // 电视初始焦点：可播放的媒体区，否则评论 / 转发标签
    val mediaFocus = item.type == DynamicType.Av || item.type == DynamicType.Pgc
    val title = if (hasMedia) item.titleText() else null
    val commentCount = item.footer?.comment ?: 0

    TvColumnPivot {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header", contentType = "header") {
                DynamicHeader(
                    item,
                    avatarSize = 44.dp,
                    nameStyle = BvType.BodyBold.copy(fontWeight = FontWeight.Bold),
                )
            }
            item(key = "media", contentType = "media") {
                if (hasMedia) {
                    DetailMedia(
                        item = item,
                        onOpen = { onOpen(item) },
                        modifier = if (mediaFocus && initialFocus != null) Modifier.focusRequester(initialFocus) else Modifier,
                    )
                } else {
                    DynamicBody(item, onOpenOrig = onOpen, onPictures = onPictures, full = true)
                }
            }
            if (title != null) {
                item(key = "title", contentType = "title") {
                    Text(
                        title,
                        style = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item(key = "tabs", contentType = "tabs") {
                BvTabRow(
                    tabs = listOf(
                        stringResource(R.string.dynamic_comment_tab, formatCount(commentCount)),
                        stringResource(R.string.dynamic_forward_tab),
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = if (!mediaFocus && initialFocus != null) Modifier.focusRequester(initialFocus) else Modifier,
                    horizontalPadding = 0.dp,
                )
            }
            if (tab == 0) {
                items(
                    commentVm.comments,
                    key = { comment -> comment.rpid },
                    contentType = { "comment" },
                ) { comment ->
                    CommentItem(
                        comment = comment,
                        modifier = if (isTv) {
                            Modifier
                                .bvFocus(scale = 1f, corner = 16.dp, addFocusable = true)
                                .padding(8.dp)
                        } else Modifier,
                        onPictureClick = onPictures,
                        onReplyClick = if (!isTv && comment.repliesCount > 0) {
                            {
                                commentVm.prepareReplies(comment)
                                showReplies = true
                                // 不随界面取消（取消会让 refreshingReplies 停在 true）
                                commentVm.viewModelScope.launch { commentVm.refreshReplies() }
                            }
                        } else null,
                    )
                }
                when {
                    commentVm.updatingComments || commentVm.refreshingComments -> item(key = "loading", contentType = "loading") {
                        LoadMoreIndicator()
                    }

                    commentVm.comments.isEmpty() -> item(key = "empty", contentType = "empty") {
                        PaneHint(stringResource(R.string.dynamic_comments_empty))
                    }
                }
            } else {
                item(key = "forward", contentType = "empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val shares = item.footer?.share ?: 0
                        if (shares > 0) PaneHint(stringResource(R.string.dynamic_forward_count, formatCount(shares)))
                        PaneHint(stringResource(R.string.dynamic_forward_unsupported))
                    }
                }
            }
        }
    }

    if (showReplies) {
        RepliesSheet(
            commentVm = commentVm,
            onShowPictures = onPictures,
            onDismiss = { showReplies = false },
        )
    }
}

/** 媒体区：高 300 圆角 24 黑底，封面 Crop；视频 / 番剧居中 64dp 播放按钮（圆角 22 primary），点击进入视频 / 番剧详情。 */
@Composable
private fun DetailMedia(item: DynamicItem, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val playable = item.type == DynamicType.Av || item.type == DynamicType.Pgc
    Box(
        modifier
            .fillMaxWidth()
            .height(300.dp)
            .bvFocus(scale = 1.03f, corner = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .then(if (playable) Modifier.clickable(onClick = onOpen) else Modifier),
    ) {
        AsyncImage(
            model = item.coverUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (playable) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.dynamic_play),
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        } else if (item.type == DynamicType.LiveRcmd) {
            LiveBadge(Modifier.align(Alignment.TopStart).padding(12.dp))
        }
    }
}

@Composable
private fun PaneHint(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, style = BvType.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
