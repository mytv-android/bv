package dev.aaa1115910.bv.ui.feature.shorts

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.ShortsItem
import dev.aaa1115910.bv.ui.designsystem.component.Ambient
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.CinemaScrim
import dev.aaa1115910.bv.ui.designsystem.component.CommentItem
import dev.aaa1115910.bv.ui.designsystem.focus.BvKey
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.focus.focusRing
import dev.aaa1115910.bv.ui.designsystem.focus.onBvKey
import dev.aaa1115910.bv.ui.designsystem.focus.toBvKey
import dev.aaa1115910.bv.ui.designsystem.icon.BvIcons
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.designsystem.theme.CoverTheme
import dev.aaa1115910.bv.ui.designsystem.theme.LocalThemePrefs
import dev.aaa1115910.bv.viewmodel.ShortsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FrameWidth = 273.dp
private val FrameHeight = 486.dp

/**
 * 电视刷刷（原型 shortsTv）：整屏氛围光 + 左侧播放队列 + 中间视频框（初始焦点）+ 右侧信息区。
 * 导航轨由外壳叠加。整页包在当前视频的 CoverTheme 里，换条时颜色动画过渡。
 *
 * 遥控器（焦点在视频框上）：↑ / 频道+ 上一条，↓ / 频道− 下一条；← 快退 5 秒（连按加速，沿用旧电视播放器）；
 * → 单按移到右侧按钮，按住或快退快进过程中继续按为快进；确认暂停 / 继续；长按确认点赞；菜单键打开评论面板。
 */
@Composable
internal fun ShortsTv(
    vm: ShortsViewModel,
    pool: ShortsPlayerPool,
    comments: ShortsComments,
    actions: ShortsActions,
) {
    val items = vm.items
    var index by rememberSaveable { mutableIntStateOf(vm.currentIndex) }
    val safeIndex = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val current = items.getOrNull(safeIndex)
    var direction by remember { mutableIntStateOf(1) }
    var panelOpen by remember { mutableStateOf(false) }
    val frameRequester = remember { FocusRequester() }
    val panelRequester = remember { FocusRequester() }
    val hearts = remember { HeartPops() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val density = LocalDensity.current
    val frameCenter = with(density) { Offset(FrameWidth.toPx() / 2, FrameHeight.toPx() / 2) }

    // 快进快退（沿用旧电视播放器：200ms 内连按计数，每 5 次步长加 5 秒）
    var seekTarget by remember { mutableLongStateOf(-1L) }
    var seekCount by remember { mutableIntStateOf(0) }
    var lastSeekAt by remember { mutableLongStateOf(0L) }
    var seekJob by remember { mutableStateOf<Job?>(null) }
    var centerPending by remember { mutableStateOf(false) }
    var rightPending by remember { mutableStateOf(false) }

    // 停稳 150ms 后开始播放当前条
    LaunchedEffect(safeIndex, current?.aid, items.getOrNull(safeIndex + 1)?.aid) {
        if (current == null) return@LaunchedEffect
        delay(SettleDelayMs)
        vm.updateCurrentIndex(safeIndex)
        pool.update(safeIndex, items)
    }
    LoadCommentsEffect(current?.aid, comments)
    // 初始焦点在视频框；评论面板打开时焦点进面板，关闭后回到视频框
    LaunchedEffect(panelOpen) {
        if (panelOpen) {
            delay(50)
            runCatching { panelRequester.requestFocus() }
        } else {
            runCatching { frameRequester.requestFocus() }
        }
    }

    val go: (Int) -> Unit = { delta ->
        val from = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val target = (from + delta).coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (target != from) {
            direction = if (delta > 0) 1 else -1
            seekJob?.cancel()
            seekTarget = -1L
            index = target
        }
    }
    val commitSeek: () -> Unit = {
        val item = items.getOrNull(index)
        val target = seekTarget
        seekJob?.cancel()
        if (item != null && target >= 0) pool.seekTo(item.aid, target)
        seekTarget = -1L
    }
    val seek: (Boolean) -> Unit = { forward ->
        val item = items.getOrNull(index)
        val slot = item?.let { pool.slotFor(it.aid) }
        if (item != null && slot != null && pool.currentAid == item.aid) {
            val now = SystemClock.uptimeMillis()
            val coefficient = if (now - lastSeekAt < 200) {
                seekCount++
                seekCount / 5
            } else {
                seekCount = 0
                0
            }
            val duration = slot.durationMs.takeIf { it > 0 } ?: (item.duration * 1000L)
            val base = if (seekTarget >= 0) seekTarget else slot.positionMs
            val step = 5000L + coefficient * 5000L
            seekTarget = (if (forward) base + step else base - step).coerceIn(0L, duration.coerceAtLeast(0L))
            lastSeekAt = now
            seekJob?.cancel()
            seekJob = scope.launch {
                delay(600)
                commitSeek()
            }
        }
    }

    CoverTheme(current?.cover, forceDark = true) {
        Box(
            Modifier
                .fillMaxSize()
                .onBvKey { key ->
                    when (key) {
                        BvKey.Menu -> {
                            panelOpen = !panelOpen
                            true
                        }

                        BvKey.ChannelUp -> {
                            go(-1)
                            true
                        }

                        BvKey.ChannelDown -> {
                            go(1)
                            true
                        }

                        else -> false
                    }
                }
        ) {
            Ambient(current?.cover)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .5f))
            )

            // 播放队列：上一条、当前、下两条
            Column(
                Modifier.offset(x = 110.dp, y = 60.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (i in (safeIndex - 1)..(safeIndex + 2)) {
                    QueueThumb(item = items.getOrNull(i), isCurrent = i == safeIndex)
                }
            }

            // 视频框
            val slideSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
            val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val slidePx = with(density) { 40.dp.roundToPx() }
            Box(
                Modifier
                    .offset(x = 300.dp, y = 27.dp)
                    .size(FrameWidth, FrameHeight)
                    .onPreviewKeyEvent { event ->
                        val key = event.toBvKey() ?: return@onPreviewKeyEvent false
                        val down = event.type == KeyEventType.KeyDown
                        val up = event.type == KeyEventType.KeyUp
                        val repeat = event.nativeKeyEvent.repeatCount
                        when (key) {
                            BvKey.Up -> {
                                if (down) go(-1)
                                true
                            }

                            BvKey.Down -> {
                                if (down) go(1)
                                true
                            }

                            BvKey.Left, BvKey.Rewind -> {
                                if (down) seek(false)
                                true
                            }

                            BvKey.FastForward -> {
                                if (down) seek(true)
                                true
                            }

                            BvKey.Right -> {
                                if (down) {
                                    val seeking = SystemClock.uptimeMillis() - lastSeekAt < 1200
                                    if (repeat > 0 || seeking) {
                                        rightPending = false
                                        seek(true)
                                    } else {
                                        rightPending = true
                                    }
                                } else if (up && rightPending) {
                                    rightPending = false
                                    focusManager.moveFocus(FocusDirection.Right)
                                }
                                true
                            }

                            BvKey.Center -> {
                                if (down && repeat == 0) {
                                    centerPending = true
                                } else if (up && centerPending) {
                                    centerPending = false
                                    if (seekTarget >= 0) commitSeek() else current?.let { pool.togglePlay(it.aid) }
                                }
                                true
                            }

                            BvKey.CenterLong -> {
                                // 长按确认：先收到普通按下（centerPending），再收到长按；部分盒子直接发长按键码（repeat = 0）
                                if (down && (centerPending || repeat == 0)) {
                                    centerPending = false
                                    current?.let {
                                        actions.likeOnly(it)
                                        if (!reduceMotion) hearts.add(frameCenter)
                                    }
                                }
                                true
                            }

                            BvKey.PlayPause -> {
                                if (down) current?.let { pool.togglePlay(it.aid) }
                                true
                            }

                            else -> false
                        }
                    }
                    .focusRequester(frameRequester)
                    .bvFocus(scale = 1.05f, corner = 24.dp, glow = 16.dp, addFocusable = true)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
            ) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        val dir = direction
                        (slideInVertically(slideSpec) { dir * slidePx } + fadeIn(fadeSpec)) togetherWith fadeOut(fadeSpec)
                    },
                    contentKey = { it?.aid },
                    label = "shortsTvFrame",
                ) { item ->
                    if (item != null) {
                        Box(Modifier.fillMaxSize()) {
                            if (!item.isPortrait) Ambient(item.cover)
                            ShortsVideo(pool, item, Modifier.fillMaxSize(), crop = item.isPortrait)
                            CinemaScrim()
                            ShortsProgress(
                                pool = pool,
                                item = item,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                seekable = false,
                                overrideMs = seekTarget.takeIf { it >= 0 && pool.currentAid == item.aid },
                            )
                        }
                    }
                }
                HeartLayer(hearts)
            }

            // 信息区
            if (current != null) {
                TvInfo(
                    item = current,
                    vm = vm,
                    comments = comments,
                    actions = actions,
                    onOpenComments = { panelOpen = true },
                    modifier = Modifier
                        .offset(x = 600.dp, y = 60.dp)
                        .width(312.dp),
                )
            }

            // 评论面板（菜单键），样式同播放器菜单面板
            AnimatedVisibility(
                visible = panelOpen && current != null,
                modifier = Modifier.align(Alignment.TopEnd),
                enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                label = "shortsTvComments",
            ) {
                BackHandler { panelOpen = false }
                Column(
                    Modifier
                        .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                        .width(360.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val item = current
                    if (item != null) {
                        val count = item.replyCount
                        Text(
                            if (count != null) stringResource(R.string.shorts_tab_comments, shortsCount(count))
                            else stringResource(R.string.shorts_comment),
                            style = BvType.PanelTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        ShortsCommentList(
                            item = item,
                            comments = comments,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            tv = true,
                            firstItemRequester = panelRequester,
                        )
                    }
                }
            }
        }
    }
}

/** 队列缩略图 54×96 圆角 12：当前项外侧 3dp primary 描边，其余 α0.55 */
@Composable
private fun QueueThumb(item: ShortsItem?, isCurrent: Boolean) {
    if (item == null) {
        Spacer(Modifier.size(54.dp, 96.dp))
        return
    }
    Box(
        Modifier
            .size(54.dp, 96.dp)
            .then(
                if (isCurrent) Modifier.focusRing(visible = true, corner = 12.dp, glow = 0.dp)
                else Modifier.alpha(0.55f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TvInfo(
    item: ShortsItem,
    vm: ShortsViewModel,
    comments: ShortsComments,
    actions: ShortsActions,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = vm.actionStateOf(item.aid)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvAvatar(url = item.author.face, size = 40.dp, fallbackText = item.author.name)
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
                    style = BvButtonStyle.Tonal,
                    icon = if (following) Icons.Filled.Check else Icons.Filled.Add,
                    height = 34.dp,
                    horizontalPadding = 14.dp,
                )
            }
        }
        Text(
            item.title,
            style = BvType.TitleLarge.copy(lineHeight = 27.5.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        val tags = item.tagsText()
        val likes = item.likeCount?.let { shortsCount(it) }
        val meta = when {
            tags.isNotEmpty() && likes != null -> stringResource(R.string.shorts_tags_like, tags, likes)
            likes != null -> stringResource(R.string.shorts_like_count, likes)
            else -> tags
        }
        if (meta.isNotEmpty()) {
            Text(
                meta,
                style = BvType.Meta.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TvActionButton(
                icon = Icons.Filled.ThumbUp,
                description = stringResource(R.string.shorts_like),
                active = state.liked,
                onClick = { actions.like(item) },
            )
            TvActionButton(
                icon = BvIcons.Coin,
                description = stringResource(R.string.shorts_coin),
                active = state.coinCount > 0,
                onClick = { actions.coin(item) },
            )
            TvActionButton(
                icon = Icons.Filled.Star,
                description = stringResource(R.string.shorts_favorite),
                active = state.favoured,
                onClick = { actions.favorite(item) },
            )
            TvActionButton(
                icon = Icons.Filled.ChatBubble,
                description = stringResource(R.string.shorts_comment),
                active = false,
                onClick = onOpenComments,
            )
            TvActionButton(
                icon = Icons.Filled.Info,
                description = stringResource(R.string.shorts_detail),
                active = false,
                onClick = { actions.openVideo(item) },
            )
        }
        val preview = if (comments.loadedAid == item.aid) comments.comments.take(2) else emptyList()
        if (preview.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .8f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                preview.forEach { CommentItem(comment = it, maxLines = 2) }
            }
        }
    }
}

/** 40dp 图标按钮：选中时 primary 背景 + 圆角 16（圆形 → 方形，fastSpatialSpec） */
@Composable
private fun TvActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val spec: FiniteAnimationSpec<Dp> = if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec()
    val corner by animateDpAsState(if (active) 16.dp else 20.dp, spec, label = "shortsTvAction")
    BvIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        style = if (active) BvIconButtonStyle.Filled else BvIconButtonStyle.Tonal,
        shape = RoundedCornerShape(corner),
        focusCorner = corner,
    )
}
