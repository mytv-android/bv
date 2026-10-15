package dev.aaa1115910.bv.ui.feature.shorts

import android.content.res.Configuration
import android.os.Build
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.ShortsItem
import dev.aaa1115910.bv.ui.designsystem.component.Ambient
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.CinemaScrim
import dev.aaa1115910.bv.ui.designsystem.icon.BvIcons
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.designsystem.theme.CoverTheme
import dev.aaa1115910.bv.ui.designsystem.theme.LocalThemePrefs
import dev.aaa1115910.bv.ui.navigation.LocalBottomBarPadding
import dev.aaa1115910.bv.viewmodel.ShortsViewModel

private val ActionWhite = Color.White.copy(alpha = .16f)

/**
 * 手机刷刷（原型 shortsPhone）：全屏 VerticalPager，每页包在 CoverTheme(cover, forceDark = true) 里。
 * 底部导航栏由外壳叠加（黑 α0.72），这里不让位；导航栏比原型（80dp）高时，底部元素整体上移差值。
 * 手机横过来时当前视频全屏横屏播放（[PhoneFullscreen]），竖回来恢复。
 */
@Composable
internal fun ShortsPhone(
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

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var fullscreenDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(landscape) { if (!landscape) fullscreenDismissed = false }
    var commentTarget by remember { mutableStateOf<ShortsItem?>(null) }
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navExtra = (LocalBottomBarPadding.current - 80.dp).coerceAtLeast(0.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { page -> items.getOrNull(page)?.aid ?: page.toLong() },
        ) { page ->
            val item = items.getOrNull(page) ?: return@VerticalPager
            CoverTheme(item.cover, forceDark = true) {
                PhonePage(
                    item = item,
                    vm = vm,
                    pool = pool,
                    actions = actions,
                    statusTop = statusTop,
                    navExtra = navExtra,
                    reduceMotion = reduceMotion,
                    onComments = { commentTarget = item },
                )
            }
        }
        SpeedBoostPill(
            visible = pool.boosting,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusTop + 52.dp),
        )
    }

    commentTarget?.let { item ->
        CoverTheme(item.cover, forceDark = true) {
            ShortsCommentSheet(item, comments, actions, onDismiss = { commentTarget = null })
        }
    }

    val current = items.getOrNull(pagerState.currentPage)
    if (landscape && !fullscreenDismissed && current != null) {
        PhoneFullscreen(
            item = current,
            pool = pool,
            actions = actions,
            reduceMotion = reduceMotion,
            onDismiss = { fullscreenDismissed = true },
        )
    }
}

@Composable
private fun PhonePage(
    item: ShortsItem,
    vm: ShortsViewModel,
    pool: ShortsPlayerPool,
    actions: ShortsActions,
    statusTop: Dp,
    navExtra: Dp,
    reduceMotion: Boolean,
    onComments: () -> Unit,
) {
    val hearts = remember { HeartPops() }
    val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    Box(
        Modifier
            .fillMaxSize()
            .shortsGestures(item, pool, actions, hearts, reduceMotion)
            .pointerInput(item.aid, item.author.mid) {
                // 向左滑 → UP 主空间
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -swipeThreshold) actions.openSpace(item.author.mid) },
                    onDragCancel = { total = 0f },
                ) { change, dragAmount ->
                    total += dragAmount
                    change.consume()
                }
            }
    ) {
        if (item.isPortrait) {
            ShortsVideo(pool, item, Modifier.fillMaxSize(), crop = true)
        } else {
            // 横屏视频：氛围光铺满 + 16:9 全宽，垂直居中再上移自身高度的 10%
            Ambient(item.cover)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val videoHeight = maxWidth * 9f / 16f
                ShortsVideo(
                    pool = pool,
                    item = item,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = -videoHeight * 0.1f)
                        .fillMaxWidth()
                        .height(videoHeight),
                    crop = false,
                )
            }
        }
        CinemaScrim()

        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = statusTop + 2.dp, start = 16.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.shorts_title), style = BvType.PanelTitle, color = Color.White)
            BvIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.shorts_search),
                onClick = actions::openSearch,
                contentColor = Color.White,
            )
        }

        // 右侧操作列
        PhoneActionColumn(
            item = item,
            vm = vm,
            actions = actions,
            onComments = onComments,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 150.dp + navExtra),
        )

        // 底部信息
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 76.dp, bottom = 112.dp + navExtra),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.shorts_at_up, item.author.name),
                modifier = Modifier.clickable { actions.openSpace(item.author.mid) },
                style = BvType.Section,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.title,
                modifier = Modifier.clickable { actions.openVideo(item) },
                style = BvType.Body.copy(lineHeight = 20.3.sp),
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
        }

        ShortsProgress(
            pool = pool,
            item = item,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 90.dp + navExtra),
        )
        HeartLayer(hearts)
    }
}

/** 右侧操作列：头像 46 + 点赞 / 投币 / 收藏 / 评论 / 分享（原型 `.m-acts`） */
@Composable
private fun PhoneActionColumn(
    item: ShortsItem,
    vm: ShortsViewModel,
    actions: ShortsActions,
    onComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = vm.actionStateOf(item.aid)
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BvAvatar(
            url = item.author.face,
            size = 46.dp,
            fallbackText = item.author.name,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { actions.openSpace(item.author.mid) },
        )
        SideActionButton(
            icon = Icons.Filled.ThumbUp,
            label = countOrLabel(item.likeCount, R.string.shorts_like),
            contentDescription = stringResource(R.string.shorts_like),
            active = state.liked,
            onClick = { actions.like(item) },
        )
        SideActionButton(
            icon = BvIcons.Coin,
            label = stringResource(R.string.shorts_coin),
            contentDescription = stringResource(R.string.shorts_coin),
            active = state.coinCount > 0,
            onClick = { actions.coin(item) },
        )
        SideActionButton(
            icon = Icons.Filled.Star,
            label = countOrLabel(item.favoriteCount, R.string.shorts_favorite),
            contentDescription = stringResource(R.string.shorts_favorite),
            active = state.favoured,
            onClick = { actions.favorite(item) },
        )
        SideActionButton(
            icon = Icons.Filled.ChatBubble,
            label = countOrLabel(item.replyCount, R.string.shorts_comment),
            contentDescription = stringResource(R.string.shorts_comment),
            active = false,
            onClick = onComments,
        )
        SideActionButton(
            icon = Icons.Filled.Share,
            label = stringResource(R.string.shorts_share),
            contentDescription = stringResource(R.string.shorts_share),
            active = false,
            onClick = { actions.share(item) },
        )
    }
}

/**
 * 原型 `.m-acts .b`：46dp，白 α0.16 + 背景模糊（API ≥ 31），图标白；
 * 选中时 primary / onPrimary，圆形 → 圆角 16 方形（fastSpatialSpec，减少动效时直接跳到终态）。
 */
@Composable
private fun SideActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val cornerSpec: FiniteAnimationSpec<Dp> =
        if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec()
    val corner by animateDpAsState(if (active) 16.dp else 23.dp, cornerSpec, label = "shortsActionCorner")
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else ActionWhite,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "shortsActionContainer",
    )
    val content by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "shortsActionContent",
    )
    val density = LocalDensity.current
    val labelStyle = remember(density) {
        TextStyle(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            shadow = Shadow(
                color = Color.Black.copy(alpha = .6f),
                offset = Offset(0f, with(density) { 1.dp.toPx() }),
                blurRadius = with(density) { 3.dp.toPx() },
            ),
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(corner))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !active) Modifier.blur(6.dp) else Modifier)
                    .background(container)
            )
            Icon(icon, contentDescription = contentDescription, tint = content, modifier = Modifier.size(24.dp))
        }
        Text(label, style = labelStyle, color = Color.White, maxLines = 1)
    }
}

/** 单击暂停 / 继续，双击点赞 + 爱心，长按 2 倍速（松手恢复） */
internal fun Modifier.shortsGestures(
    item: ShortsItem,
    pool: ShortsPlayerPool,
    actions: ShortsActions,
    hearts: HeartPops,
    reduceMotion: Boolean,
): Modifier = pointerInput(item.aid, reduceMotion) {
    detectTapGestures(
        onPress = {
            tryAwaitRelease()
            pool.setBoost(false)
        },
        onDoubleTap = { offset ->
            actions.likeOnly(item)
            if (!reduceMotion) hearts.add(offset)
        },
        onLongPress = {
            if (pool.currentAid == item.aid) pool.setBoost(true)
        },
        onTap = { pool.togglePlay(item.aid) },
    )
}

/** 评论：ModalBottomSheet + CommentViewModel 列表 */
@Composable
private fun ShortsCommentSheet(
    item: ShortsItem,
    comments: ShortsComments,
    actions: ShortsActions,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(item.aid) { comments.load(item.aid) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val count = item.replyCount
        Text(
            if (count != null) stringResource(R.string.shorts_tab_comments, shortsCount(count))
            else stringResource(R.string.shorts_comment),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            style = BvType.Section,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ShortsCommentList(
            item = item,
            comments = comments,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            onOpenSpace = { mid ->
                onDismiss()
                actions.openSpace(mid)
            },
        )
    }
}

/**
 * 手机横过来：当前视频全屏横屏播放。外壳的底部导航栏仍在，所以用全屏 Dialog 盖住并隐藏系统栏；
 * 返回键关闭（直到再次竖过来）。
 */
@Composable
private fun PhoneFullscreen(
    item: ShortsItem,
    pool: ShortsPlayerPool,
    actions: ShortsActions,
    reduceMotion: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                WindowCompat.getInsetsController(window, view).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose { }
        }
        CoverTheme(item.cover, forceDark = true) {
            val hearts = remember(item.aid) { HeartPops() }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .shortsGestures(item, pool, actions, hearts, reduceMotion)
            ) {
                ShortsVideo(pool, item, Modifier.fillMaxSize(), crop = false)
                ShortsProgress(
                    pool = pool,
                    item = item,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                )
                SpeedBoostPill(
                    visible = pool.boosting,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                )
                HeartLayer(hearts)
            }
        }
    }
}
