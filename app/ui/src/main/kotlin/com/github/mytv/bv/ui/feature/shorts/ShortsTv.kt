package com.github.mytv.bv.ui.feature.shorts

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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
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
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.ShortsItem
import com.github.mytv.bv.ui.designsystem.component.Ambient
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvIconButton
import com.github.mytv.bv.ui.designsystem.component.BvIconButtonStyle
import com.github.mytv.bv.ui.designsystem.component.CinemaScrim
import com.github.mytv.bv.ui.designsystem.component.CommentItem
import com.github.mytv.bv.ui.designsystem.focus.BvKey
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.focus.focusRing
import com.github.mytv.bv.ui.designsystem.focus.onBvKey
import com.github.mytv.bv.ui.designsystem.focus.toBvKey
import com.github.mytv.bv.ui.designsystem.icon.BvIcons
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.designsystem.theme.CoverTheme
import com.github.mytv.bv.ui.designsystem.theme.LocalThemePrefs
import com.github.mytv.bv.ui.navigation.LocalTvRailReveal
import com.github.mytv.bv.viewmodel.ShortsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 竖屏视频顶天立地：宽度按屏幕高度 9:16 计算（960×540 画布下为 304×540）。 */
private val PortraitWidth = 304.dp
private val PortraitX = 300.dp

/** 横屏信息层 3 秒无操作后自动隐藏；隐藏后按任意方向键唤回。 */
private const val InfoAutoHideMs = 3_000L

/** 按住 ← 多久后开始快退；短按则把焦点移回左侧导航轨。 */
private const val LeftHoldSeekMs = 350L

/**
 * 电视刷刷（原型 shortsTv）：整屏氛围光 + 视频舞台（初始焦点）。
 * 竖屏视频顶天立地（304×540），左侧播放队列、右侧信息列；
 * 横屏视频直接铺满整屏，标题 / UP / 操作按钮 / 进度条叠在底部渐变上；
 * 横屏播放 3 秒无操作后信息层整体隐藏，只剩画面，任意方向键唤回。
 * 导航轨由外壳叠加，播放中自动隐藏，← 或左缘右滑手势呼出。
 * 整页包在当前视频的 CoverTheme 里，换条时颜色动画过渡。
 *
 * 遥控器（焦点在视频框上）：↑ / 频道+ 上一条，↓ / 频道− 下一条；← 单按呼出导航轨，
 * 按住才快退 5 秒并连按加速（媒体快退键始终快退）；→ 单按移到操作按钮（横屏为底部信息层），
 * 按住或快退快进过程中继续按为快进；
 * 确认暂停 / 继续；长按确认点赞；菜单键打开评论面板。
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
    val landscapeActionsRequester = remember { FocusRequester() }
    val hearts = remember { HeartPops() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val density = LocalDensity.current
    // 优先用播放器上报的真实宽高判断横竖屏：story 流的元数据可能缺失或不准，之前会把横屏视频裁成竖屏
    val currentSlot = current?.let { pool.slotFor(it.aid) }
    val videoWidth = currentSlot?.videoWidth?.takeIf { it > 0 } ?: (current?.width ?: 0)
    val videoHeight = currentSlot?.videoHeight?.takeIf { it > 0 } ?: (current?.height ?: 0)
    val landscape = videoHeight > 0 && videoWidth > videoHeight
    val portrait = !landscape
    var infoVisible by remember { mutableStateOf(true) }
    var infoFocused by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    val playing = currentSlot?.isPlaying == true && !pool.userPaused
    val revealInfo: () -> Unit = {
        infoVisible = true
        interactionTick++
    }
    val revealRail = LocalTvRailReveal.current

    // 快进快退（沿用旧电视播放器：200ms 内连按计数，每 5 次步长加 5 秒）
    var seekTarget by remember { mutableLongStateOf(-1L) }
    var seekCount by remember { mutableIntStateOf(0) }
    var lastSeekAt by remember { mutableLongStateOf(0L) }
    var seekJob by remember { mutableStateOf<Job?>(null) }
    var centerPending by remember { mutableStateOf(false) }
    var rightPending by remember { mutableStateOf(false) }
    // ←：短按回导航轨，按住才快退（快退任务在按住 350ms 后启动）
    var leftPending by remember { mutableStateOf(false) }
    var leftSeeking by remember { mutableStateOf(false) }
    var leftHoldJob by remember { mutableStateOf<Job?>(null) }

    // 停稳 150ms 后开始播放当前条
    LaunchedEffect(safeIndex, current?.aid, items.getOrNull(safeIndex + 1)?.aid) {
        if (current == null) return@LaunchedEffect
        delay(SettleDelayMs)
        vm.updateCurrentIndex(safeIndex)
        pool.update(safeIndex, items)
    }
    LoadCommentsEffect(current?.aid, comments)
    // 初始焦点在视频框；评论面板打开时焦点进面板，关闭后回到视频框。
    // 页面进入时的请求可能撞上转场被丢掉，重试几次，否则焦点留在导航轨上、自动隐藏不生效。
    LaunchedEffect(panelOpen) {
        if (panelOpen) {
            delay(50)
            runCatching { panelRequester.requestFocus() }
        } else {
            repeat(6) {
                withFrameNanos { }
                if (runCatching { frameRequester.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
                delay(120)
            }
        }
    }
    // 横屏播放中 3 秒无操作后隐藏整个信息层；暂停、焦点在信息层上时保持可见
    LaunchedEffect(landscape, safeIndex, current?.aid, interactionTick, infoFocused, playing) {
        if (!landscape || infoFocused || !playing) {
            infoVisible = true
            return@LaunchedEffect
        }
        infoVisible = true
        delay(InfoAutoHideMs)
        infoVisible = false
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
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                // 电视遥控器的「手势」：从屏幕左缘向右滑，呼出导航轨
                .pointerInput(revealRail) {
                    val edgeWidth = size.width * 0.12f
                    val threshold = 64.dp.toPx()
                    var fromEdge = false
                    var distance = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            fromEdge = offset.x <= edgeWidth
                            distance = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (fromEdge) {
                                distance += dragAmount
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (fromEdge && distance > threshold) {
                                revealInfo()
                                revealRail?.invoke()
                            }
                            fromEdge = false
                        },
                        onDragCancel = { fromEdge = false },
                    )
                }
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
            val screenHeight = maxHeight
            val frameCenter = with(density) {
                if (landscape) Offset(maxWidth.toPx() / 2, screenHeight.toPx() / 2)
                else Offset((PortraitX + PortraitWidth * 0.5f).toPx(), screenHeight.toPx() / 2)
            }

            // 横屏视频框铺满整屏且有不透明黑底，氛围光完全不可见；只在竖屏绘制。
            // 原来的 50% surface 遮罩折进 Ambient 的 alpha（0.55 × 0.5 = 0.275），省掉一层全屏混合。
            if (portrait) Ambient(current?.cover, alpha = 0.275f)

            // 播放队列只在竖屏保留；横屏全屏时收起
            if (portrait) {
                Column(
                    Modifier.offset(x = 110.dp, y = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (i in (safeIndex - 1)..(safeIndex + 2)) {
                        QueueThumb(item = items.getOrNull(i), isCurrent = i == safeIndex)
                    }
                }
            }

            // 视频框
            val slideSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
            val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val slidePx = with(density) { 40.dp.roundToPx() }
            Box(
                Modifier
                    .then(
                        if (portrait) Modifier
                            .offset(x = PortraitX)
                            .width(PortraitWidth)
                            .fillMaxHeight()
                        else Modifier.fillMaxSize()
                    )
                    .onPreviewKeyEvent { event ->
                        val key = event.toBvKey() ?: return@onPreviewKeyEvent false
                        val down = event.type == KeyEventType.KeyDown
                        val up = event.type == KeyEventType.KeyUp
                        val repeat = event.nativeKeyEvent.repeatCount
                        // 任意按键都把横屏信息层唤回，并重新计时
                        if (down && repeat == 0) revealInfo()
                        when (key) {
                            BvKey.Up -> {
                                if (down) go(-1)
                                true
                            }

                            BvKey.Down -> {
                                if (down) go(1)
                                true
                            }

                            BvKey.Left -> {
                                if (down) {
                                    if (repeat == 0) {
                                        leftPending = true
                                        leftSeeking = false
                                        leftHoldJob?.cancel()
                                        leftHoldJob = scope.launch {
                                            delay(LeftHoldSeekMs)
                                            if (leftPending) {
                                                leftSeeking = true
                                                seek(false)
                                            }
                                        }
                                    } else if (leftSeeking) {
                                        seek(false)
                                    }
                                } else if (up) {
                                    leftHoldJob?.cancel()
                                    leftHoldJob = null
                                    val tap = leftPending && !leftSeeking
                                    leftPending = false
                                    leftSeeking = false
                                    // 短按 ← 呼出导航轨。横屏画面铺满整屏，2D 焦点搜索找不到「更左边」的导轨项，
                                    // moveFocus(Left) 会失败，所以直接请求导轨焦点（与外壳返回键同一通路）。
                                    if (tap) {
                                        revealInfo()
                                        revealRail?.invoke()
                                    }
                                }
                                true
                            }

                            BvKey.Rewind -> {
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
                                    if (landscape) {
                                        // 信息层可能已经自动隐藏，先唤回再请求焦点
                                        revealInfo()
                                        scope.launch {
                                            withFrameNanos { }
                                            if (!runCatching { landscapeActionsRequester.requestFocus() }.getOrDefault(false)) {
                                                focusManager.moveFocus(FocusDirection.Right)
                                            }
                                        }
                                    } else {
                                        focusManager.moveFocus(FocusDirection.Right)
                                    }
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
                    .bvFocus(scale = if (landscape) 1f else 1.05f, corner = 0.dp, glow = 16.dp, addFocusable = true)
                    .clip(RoundedCornerShape(0.dp))
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
                            ShortsVideo(pool, item, Modifier.fillMaxSize(), crop = !landscape)
                            // 竖屏框常驻上下渐变（进度条可读性）；横屏的渐变在信息层里，随隐藏一起淡出
                            if (portrait) {
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
                }
                HeartLayer(hearts)
            }

            // 信息区：竖屏在右侧常驻；横屏是画面底部的信息层，播放 3 秒无操作后整体淡出
            if (current != null) {
                if (portrait) {
                    TvInfo(
                        item = current,
                        vm = vm,
                        comments = comments,
                        actions = actions,
                        onOpenComments = { panelOpen = true },
                        modifier = Modifier
                            .offset(x = 632.dp, y = 60.dp)
                            .width(280.dp),
                    )
                } else {
                    AnimatedVisibility(
                        visible = infoVisible,
                        enter = fadeIn(fadeSpec),
                        exit = fadeOut(fadeSpec),
                        // 聚焦的视频框带 zIndex(1f)，信息层必须压在它上面
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .zIndex(2f),
                    ) {
                        TvLandscapeInfo(
                            item = current,
                            vm = vm,
                            pool = pool,
                            actions = actions,
                            onOpenComments = { panelOpen = true },
                            firstActionRequester = landscapeActionsRequester,
                            frameRequester = frameRequester,
                            seekOverrideMs = seekTarget.takeIf { it >= 0 && pool.currentAid == current.aid },
                            onFocusChanged = { infoFocused = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 评论面板（菜单键），样式同播放器菜单面板
            AnimatedVisibility(
                visible = panelOpen && current != null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(3f),
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
        val meta = item.metaLine()
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

/**
 * 横屏视频的全屏信息层：底部渐变上叠 UP 主 / 标题 / 5 个操作按钮和进度条。
 * 播放 3 秒无操作后由外层整体淡出；→ 由外层请求 firstActionRequester，← 从第一个按钮或关注按钮回到视频。
 */
@Composable
private fun TvLandscapeInfo(
    item: ShortsItem,
    vm: ShortsViewModel,
    pool: ShortsPlayerPool,
    actions: ShortsActions,
    onOpenComments: () -> Unit,
    firstActionRequester: FocusRequester,
    frameRequester: FocusRequester,
    seekOverrideMs: Long?,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = vm.actionStateOf(item.aid)
    val following = vm.isFollowing(item.author.mid) == true
    val meta = item.metaLine()
    val likeLabel = item.likeCount?.let { shortsCount(it) }.orEmpty()
    val favoriteLabel = item.favoriteCount?.let { shortsCount(it) }.orEmpty()
    val commentLabel = item.replyCount?.let { shortsCount(it) }.orEmpty()
    Box(
        modifier
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Black.copy(alpha = .55f),
                    1f to Color.Black.copy(alpha = .9f),
                )
            )
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .padding(start = 96.dp, end = 48.dp, top = 120.dp, bottom = 27.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BvAvatar(url = item.author.face, size = 44.dp, fallbackText = item.author.name)
                        Column(
                            Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                item.author.name,
                                style = BvType.Body.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            item.authorFans?.let { fans ->
                                Text(
                                    stringResource(R.string.shorts_fans, shortsCount(fans)),
                                    style = BvType.Meta,
                                    color = Color.White.copy(alpha = .8f),
                                    maxLines = 1,
                                )
                            }
                        }
                        if (item.author.mid > 0) {
                            BvButton(
                                text = stringResource(if (following) R.string.shorts_followed else R.string.shorts_follow),
                                onClick = { actions.follow(item) },
                                modifier = Modifier.focusProperties { left = frameRequester },
                                style = BvButtonStyle.Tonal,
                                icon = if (following) Icons.Filled.Check else Icons.Filled.Add,
                                height = 34.dp,
                                horizontalPadding = 14.dp,
                            )
                        }
                    }
                    Text(
                        item.title,
                        style = BvType.Display.copy(fontSize = 32.sp, lineHeight = 38.sp),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (meta.isNotEmpty()) {
                        Text(
                            meta,
                            style = BvType.Meta.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = Color.White.copy(alpha = .85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LandscapeAction(
                        icon = Icons.Filled.ThumbUp,
                        description = stringResource(R.string.shorts_like),
                        label = likeLabel,
                        active = state.liked,
                        onClick = { actions.like(item) },
                        modifier = Modifier
                            .focusRequester(firstActionRequester)
                            .focusProperties { left = frameRequester },
                    )
                    LandscapeAction(
                        icon = BvIcons.Coin,
                        description = stringResource(R.string.shorts_coin),
                        label = stringResource(R.string.shorts_coin),
                        active = state.coinCount > 0,
                        onClick = { actions.coin(item) },
                    )
                    LandscapeAction(
                        icon = Icons.Filled.Star,
                        description = stringResource(R.string.shorts_favorite),
                        label = favoriteLabel,
                        active = state.favoured,
                        onClick = { actions.favorite(item) },
                    )
                    LandscapeAction(
                        icon = Icons.Filled.ChatBubble,
                        description = stringResource(R.string.shorts_comment),
                        label = commentLabel,
                        active = false,
                        onClick = onOpenComments,
                    )
                    LandscapeAction(
                        icon = Icons.Filled.Info,
                        description = stringResource(R.string.shorts_detail),
                        label = stringResource(R.string.shorts_detail),
                        active = false,
                        onClick = { actions.openVideo(item) },
                    )
                }
            }
            ShortsProgress(
                pool = pool,
                item = item,
                modifier = Modifier.fillMaxWidth(),
                seekable = false,
                overrideMs = seekOverrideMs,
            )
        }
    }
}

/** 横屏信息层的操作按钮：图标 + 下方计数 / 文案。 */
@Composable
private fun LandscapeAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TvActionButton(
            icon = icon,
            description = description,
            active = active,
            onClick = onClick,
            modifier = modifier,
        )
        if (label.isNotEmpty()) {
            Text(
                label,
                style = BvType.Meta.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = .9f),
                maxLines = 1,
            )
        }
    }
}

/** 「标签　x 点赞」/「x 点赞」/ 标签（竖屏信息列与横屏信息条共用）。 */
@Composable
private fun ShortsItem.metaLine(): String {
    val tags = tagsText()
    val likes = likeCount?.let { shortsCount(it) }
    return when {
        tags.isNotEmpty() && likes != null -> stringResource(R.string.shorts_tags_like, tags, likes)
        likes != null -> stringResource(R.string.shorts_like_count, likes)
        else -> tags
    }
}

/** 40dp 图标按钮：选中时 primary 背景 + 圆角 16（圆形 → 方形，fastSpatialSpec） */
@Composable
private fun TvActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalThemePrefs.current.reduceMotion
    val spec: FiniteAnimationSpec<Dp> = if (reduceMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec()
    val corner by animateDpAsState(if (active) 16.dp else 20.dp, spec, label = "shortsTvAction")
    BvIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        modifier = modifier,
        style = if (active) BvIconButtonStyle.Filled else BvIconButtonStyle.Tonal,
        shape = RoundedCornerShape(corner),
        focusCorner = corner,
    )
}
