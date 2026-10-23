package dev.aaa1115910.bv.ui.feature.shorts

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.ShortsItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvListItem
import dev.aaa1115910.bv.ui.designsystem.component.CommentItem
import dev.aaa1115910.bv.ui.designsystem.component.FullPageLoading
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.component.PageMessage
import dev.aaa1115910.bv.ui.designsystem.component.WavyProgress
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.icon.BvIcons
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.navigation.SearchRoute
import dev.aaa1115910.bv.ui.navigation.SpaceRoute
import dev.aaa1115910.bv.ui.navigation.VideoRoute
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.ShortsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

/** 页面停稳多久后才开始播放当前页（第 10.4 节） */
internal const val SettleDelayMs = 150L

// ---------------------------------------------------------------- 操作

/**
 * 点赞 / 投币 / 收藏 / 分享 / 关注 与跳转。投币、收藏会先弹对话框（[ShortsActionDialogs]）。
 */
@Stable
internal class ShortsActions(
    private val context: Context,
    val vm: ShortsViewModel,
    private val nav: NavController,
) {
    var coinTarget by mutableStateOf<ShortsItem?>(null)
        private set
    var favoriteTarget by mutableStateOf<ShortsItem?>(null)
        private set

    fun like(item: ShortsItem) = vm.toggleLike(item.aid)

    /** 双击 / 长按确认：只点赞不取消 */
    fun likeOnly(item: ShortsItem) = vm.like(item.aid)

    fun coin(item: ShortsItem) {
        if (!vm.isLoggedIn) {
            toast(R.string.shorts_toast_login)
            return
        }
        if (vm.actionStateOf(item.aid).coinCount >= 2) {
            toast(R.string.shorts_coin_full)
            return
        }
        coinTarget = item
    }

    fun favorite(item: ShortsItem) {
        if (!vm.isLoggedIn) {
            toast(R.string.shorts_toast_login)
            return
        }
        vm.loadFavoriteFolders(item.aid)
        favoriteTarget = item
    }

    fun follow(item: ShortsItem) = vm.toggleFollow(item.author.mid)

    fun share(item: ShortsItem) {
        val id = item.bvid?.takeIf { it.isNotBlank() } ?: "av${item.aid}"
        val url = "https://www.bilibili.com/video/$id"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, item.title)
            putExtra(Intent.EXTRA_TEXT, "${item.title} $url")
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, context.getString(R.string.shorts_share_chooser)))
        }
    }

    fun openSpace(mid: Long) {
        if (mid > 0) nav.navigate(SpaceRoute(mid))
    }

    fun openVideo(item: ShortsItem) = nav.navigate(VideoRoute(item.aid))

    fun openSearch() = nav.navigate(SearchRoute())

    internal fun dismissCoin() {
        coinTarget = null
    }

    internal fun dismissFavorite() {
        favoriteTarget = null
    }

    private fun toast(@StringRes res: Int) = Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
}

@Composable
internal fun rememberShortsActions(vm: ShortsViewModel, nav: NavController): ShortsActions {
    val context = LocalContext.current
    return remember(vm, nav, context) { ShortsActions(context, vm, nav) }
}

/** 投币（1 / 2 枚）与收藏夹多选对话框 */
@Composable
internal fun ShortsActionDialogs(actions: ShortsActions) {
    actions.coinTarget?.let { item ->
        val remaining = 2 - actions.vm.actionStateOf(item.aid).coinCount
        CoinDialog(
            maxCoins = remaining.coerceIn(1, 2),
            onConfirm = { count, alsoLike ->
                actions.vm.coin(item.aid, count, alsoLike)
                actions.dismissCoin()
            },
            onDismiss = actions::dismissCoin,
        )
    }
    actions.favoriteTarget?.let { item ->
        FavoriteDialog(item = item, vm = actions.vm, onDismiss = actions::dismissFavorite)
    }
}

@Composable
private fun CoinDialog(
    maxCoins: Int,
    onConfirm: (count: Int, alsoLike: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var count by remember { mutableIntStateOf(1) }
    var alsoLike by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(count.coerceAtMost(maxCoins), alsoLike) }) {
                Text(stringResource(R.string.shorts_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.shorts_cancel)) }
        },
        title = { Text(stringResource(R.string.shorts_coin_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BvChip(
                        text = stringResource(R.string.shorts_coin_one),
                        selected = count == 1,
                        onClick = { count = 1 },
                    )
                    if (maxCoins >= 2) {
                        BvChip(
                            text = stringResource(R.string.shorts_coin_two),
                            selected = count == 2,
                            onClick = { count = 2 },
                        )
                    }
                }
                BvListItem(
                    title = stringResource(R.string.shorts_coin_also_like),
                    onClick = { alsoLike = !alsoLike },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    trailing = { Checkbox(checked = alsoLike, onCheckedChange = null) },
                )
            }
        },
    )
}

@Composable
private fun FavoriteDialog(item: ShortsItem, vm: ShortsViewModel, onDismiss: () -> Unit) {
    val folders = vm.favoriteFolders
    val loading = vm.favoriteFoldersLoading
    var selected by remember(item.aid) { mutableStateOf(emptySet<Long>()) }
    var initialized by remember(item.aid) { mutableStateOf(false) }
    LaunchedEffect(item.aid, loading, folders.size) {
        if (!initialized && !loading && vm.favoriteFoldersAid == item.aid && folders.isNotEmpty()) {
            selected = folders.filter { it.videoInThisFav }.map { it.id }.toSet()
            initialized = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    vm.updateFavoriteFolders(item.aid, selected.toList())
                    onDismiss()
                },
                enabled = initialized,
            ) { Text(stringResource(R.string.shorts_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.shorts_cancel)) }
        },
        title = { Text(stringResource(R.string.shorts_favorite_title)) },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                folders.isEmpty() -> Text(
                    stringResource(R.string.shorts_favorite_empty),
                    style = BvType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn(
                    Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(
                        items = folders,
                        key = { it.id },
                        contentType = { "folder" },
                    ) { folder ->
                        val checked = folder.id in selected
                        BvListItem(
                            title = folder.title,
                            subtitle = stringResource(R.string.shorts_favorite_count, folder.mediaCount),
                            onClick = {
                                selected = if (checked) selected - folder.id else selected + folder.id
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            trailing = { Checkbox(checked = checked, onCheckedChange = null) },
                        )
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------- 评论

/**
 * 包一层 [CommentViewModel]：按 aid 加载评论（视频评论 type = 1），串行执行，切换视频时只保留最后一次请求。
 */
@Stable
internal class ShortsComments(
    val vm: CommentViewModel,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private var requestedAid = 0L

    /** 当前列表属于哪个视频 */
    var loadedAid by mutableLongStateOf(0L)
        private set

    val comments: List<Comment> get() = vm.comments
    val replies: List<Comment> get() = vm.replies
    val loading: Boolean get() = vm.updatingComments || vm.refreshingComments
    val loadingReplies: Boolean get() = vm.updatingReplies || vm.refreshingReplies

    fun load(aid: Long) {
        requestedAid = aid
        scope.launch {
            mutex.withLock {
                if (aid != requestedAid || loadedAid == aid) return@withLock
                vm.commentId = aid
                vm.commentType = 1L
                loadedAid = aid
                vm.refreshComments()
            }
        }
    }

    fun loadMore() {
        if (loadedAid == 0L || vm.updatingComments || !vm.hasMoreComments) return
        scope.launch { mutex.withLock { vm.loadMoreComment() } }
    }

    fun openReplies(comment: Comment) {
        vm.rpid = comment.rpid
        vm.replyRootComment = comment
        vm.replies.clear()
        scope.launch { mutex.withLock { vm.refreshReplies() } }
    }

    fun loadMoreReplies() {
        if (vm.updatingReplies || !vm.hasMoreReplies) return
        scope.launch { mutex.withLock { vm.loadMoreReplies() } }
    }
}

@Composable
internal fun rememberShortsComments(vm: CommentViewModel): ShortsComments {
    val scope = rememberCoroutineScope()
    return remember(vm, scope) { ShortsComments(vm, scope) }
}

/** 当前视频变化后（停留 400ms）加载它的评论：平板右栏、电视评论卡用 */
@Composable
internal fun LoadCommentsEffect(aid: Long?, comments: ShortsComments) {
    LaunchedEffect(aid) {
        if (aid == null) return@LaunchedEffect
        delay(400)
        comments.load(aid)
    }
}

/**
 * 评论列表（原型 comment()）。触屏可以点「回复 N」看楼中楼；电视上每条评论整体可聚焦，用于滚动。
 */
@Composable
internal fun ShortsCommentList(
    item: ShortsItem,
    comments: ShortsComments,
    modifier: Modifier = Modifier,
    state: LazyListState = remember(item.aid) { LazyListState() },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    tv: Boolean = false,
    firstItemRequester: FocusRequester? = null,
    onOpenSpace: (Long) -> Unit = {},
) {
    var replyRoot by remember(item.aid) { mutableStateOf<Comment?>(null) }
    BackHandler(enabled = replyRoot != null) { replyRoot = null }
    val root = replyRoot
    val list = if (root == null) comments.comments else comments.replies
    val loading = if (root == null) comments.loading else comments.loadingReplies
    val ready = comments.loadedAid == item.aid
    val latestRoot by rememberUpdatedState(root)

    LaunchedEffect(state, item.aid) {
        // 带上总数：一页填不满屏幕时，加载完成后继续加载下一页
        snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            (info.totalItemsCount > 0 && last > info.totalItemsCount - 6) to info.totalItemsCount
        }.distinctUntilChanged().collectLatest { (near, _) ->
            if (!near) return@collectLatest
            if (latestRoot == null) comments.loadMore() else comments.loadMoreReplies()
        }
    }

    val content: @Composable () -> Unit = {
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (root != null) {
                item(key = "replyHeader") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BvIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.ds_back),
                            onClick = { replyRoot = null },
                            size = 36.dp,
                            iconSize = 20.dp,
                        )
                        Text(stringResource(R.string.shorts_replies), style = BvType.Section, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                item(key = "replyRoot") {
                    CommentItem(comment = root, onAvatarClick = { onOpenSpace(root.member.mid) })
                }
                item(key = "replyDivider") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (ready || root != null) {
                itemsIndexed(
                    items = list,
                    key = { _, comment -> comment.rpid },
                    contentType = { _, _ -> "comment" },
                ) { index, comment ->
                    val focusModifier = if (tv) {
                        Modifier
                            .then(if (index == 0 && firstItemRequester != null) Modifier.focusRequester(firstItemRequester) else Modifier)
                            .bvFocus(scale = 1f, corner = 16.dp, addFocusable = true)
                    } else {
                        Modifier
                    }
                    CommentItem(
                        comment = comment,
                        modifier = focusModifier,
                        onReplyClick = if (!tv && root == null && comment.repliesCount > 0) {
                            {
                                replyRoot = comment
                                comments.openReplies(comment)
                            }
                        } else {
                            null
                        },
                        onAvatarClick = if (!tv) {
                            { onOpenSpace(comment.member.mid) }
                        } else {
                            null
                        },
                    )
                }
            }
            if (loading || !ready) {
                item(key = "loading") { LoadMoreIndicator() }
            } else if (list.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.shorts_no_comments),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        style = BvType.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (tv) TvColumnPivot(content) else content()
}

// ---------------------------------------------------------------- 双击爱心

@Immutable
internal data class HeartPop(val id: Long, val x: Float, val y: Float, val rotation: Float)

@Stable
internal class HeartPops {
    val items = mutableStateListOf<HeartPop>()
    private var nextId = 0L

    fun add(offset: Offset) {
        items.add(HeartPop(nextId++, offset.x, offset.y, Random.nextFloat() * 24f - 12f))
    }

    fun remove(pop: HeartPop) {
        items.remove(pop)
    }
}

/** 双击点赞的爱心层：96dp 爱心在点击位置 0→1.15→1 弹出并轻微旋转，然后上移 60dp 淡出，共 900ms */
@Composable
internal fun HeartLayer(pops: HeartPops, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        pops.items.forEach { pop ->
            key(pop.id) { HeartPopItem(pop, onDone = { pops.remove(pop) }) }
        }
    }
}

@Composable
private fun HeartPopItem(pop: HeartPop, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 900, easing = LinearEasing))
        done()
    }
    val density = LocalDensity.current
    val half = with(density) { 48.dp.toPx() }
    val rise = with(density) { 60.dp.toPx() }
    Icon(
        BvIcons.Heart,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .offset { IntOffset((pop.x - half).roundToInt(), (pop.y - half).roundToInt()) }
            .size(96.dp)
            .graphicsLayer {
                val t = progress.value
                val scale = when {
                    t < 0.25f -> 1.15f * FastOutSlowInEasing.transform(t / 0.25f)
                    t < 0.4f -> 1.15f - 0.15f * ((t - 0.25f) / 0.15f)
                    else -> 1f
                }
                val out = ((t - 0.4f) / 0.6f).coerceIn(0f, 1f)
                scaleX = scale
                scaleY = scale
                rotationZ = pop.rotation
                translationY = -rise * FastOutSlowInEasing.transform(out)
                alpha = 1f - out
            },
    )
}

// ---------------------------------------------------------------- 小组件

/** 长按时顶部中间的「2× 倍速中」小标签 */
@Composable
internal fun SpeedBoostPill(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        label = "speedBoost",
    ) {
        Text(
            stringResource(R.string.shorts_speed_boost),
            modifier = Modifier
                .background(Color.Black.copy(alpha = .55f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = BvType.Label,
            color = Color.White,
        )
    }
}

/**
 * 当前视频的波浪进度条。[seekable] 时可拖动；[overrideMs] 用于电视快进快退预览。
 */
@Composable
internal fun ShortsProgress(
    pool: ShortsPlayerPool,
    item: ShortsItem,
    modifier: Modifier = Modifier,
    seekable: Boolean = true,
    overrideMs: Long? = null,
) {
    val slot = pool.slotFor(item.aid)
    var seeking by remember(item.aid) { mutableStateOf<Float?>(null) }
    val duration: () -> Long = {
        slot?.durationMs?.takeIf { it > 0 } ?: (item.duration * 1000L)
    }
    val onSeek: ((Float) -> Unit)? = if (seekable) {
        { value: Float -> seeking = value }
    } else {
        null
    }
    val onSeekFinished: (() -> Unit)? = if (seekable) {
        {
            seeking?.let { pool.seekTo(item.aid, (it * duration()).toLong()) }
            seeking = null
        }
    } else {
        null
    }
    WavyProgress(
        // 位置只在 Draw 阶段读取：播放器每 100ms 更新 positionMs 不会触发重组
        progress = {
            seeking ?: run {
                val total = duration()
                val position = overrideMs ?: slot?.positionMs ?: 0L
                if (total > 0) position.toFloat() / total else 0f
            }
        },
        modifier = modifier,
        playing = slot?.isPlaying == true,
        onSeek = onSeek,
        onSeekFinished = onSeekFinished,
    )
}

/** 列表为空时：加载中 / 失败或没有内容（可重试） */
@Composable
internal fun ShortsStatus(vm: ShortsViewModel, started: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        if (!started || vm.initialLoading || vm.loading) {
            FullPageLoading()
        } else {
            PageMessage(
                message = vm.loadError ?: stringResource(R.string.shorts_empty),
                onRetry = { vm.refresh() },
            )
        }
    }
}

/**
 * 手机 / 平板：翻页停稳 150ms 后更新当前条并交给播放器池（停稳前继续播放上一条）
 */
@Composable
internal fun PagerSettleEffect(pagerState: PagerState, vm: ShortsViewModel, pool: ShortsPlayerPool) {
    val items = vm.items
    LaunchedEffect(pagerState, vm, pool) {
        snapshotFlow {
            if (pagerState.isScrollInProgress) {
                null
            } else {
                val page = pagerState.currentPage
                Triple(page, items.getOrNull(page)?.aid, items.getOrNull(page + 1)?.aid)
            }
        }.collectLatest { key ->
            if (key == null || key.second == null) return@collectLatest
            delay(SettleDelayMs)
            vm.updateCurrentIndex(key.first)
            pool.update(key.first, items)
        }
    }
}

// ---------------------------------------------------------------- 文本

/** 「12.3万」「1.2亿」 */
@Composable
internal fun shortsCount(count: Long): String = when {
    count >= 100_000_000L -> stringResource(R.string.shorts_count_yi, oneDecimal(count / 100_000_000.0))
    count >= 10_000L -> stringResource(R.string.shorts_count_wan, oneDecimal(count / 10_000.0))
    else -> count.coerceAtLeast(0L).toString()
}

/** 数量未知（推荐流回退）时显示文字标签 */
@Composable
internal fun countOrLabel(count: Long?, @StringRes label: Int): String =
    if (count != null) shortsCount(count) else stringResource(label)

private fun oneDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value).removeSuffix(".0")

/** 「#夜跑 #城市」 */
internal fun ShortsItem.tagsText(): String =
    tags.filter { it.isNotBlank() }.take(3).joinToString(" ") { "#$it" }

/** 发布时间：今天 05:12 / 昨天 05:12 / 10-02 / 2023-10-02 */
@Composable
internal fun shortsPubdate(seconds: Long): String {
    val date = Date(seconds * 1000)
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    return when {
        sameDay(now, then) -> stringResource(R.string.shorts_today, time)
        sameDay(yesterday, then) -> stringResource(R.string.shorts_yesterday, time)
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

internal fun ShortsItem.toCardData() = VideoCardData(
    avid = aid,
    title = title,
    cover = cover,
    upName = author.name,
    play = viewCount?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt(),
    time = duration * 1000L,
    upMid = author.mid.takeIf { it > 0 },
)
