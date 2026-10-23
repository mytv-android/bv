package dev.aaa1115910.bv.ui.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.adaptive.WidthClass
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.LoadMoreIndicator
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.home.EmptyOrLoading
import dev.aaa1115910.bv.ui.feature.home.LoadMoreEffect
import kotlinx.coroutines.delay

/*
 * 第 9.11 节模板的「页面框架 + 网格」（历史、收藏、稍后再看、追番、关注、UP 主空间、Tag、账号管理共用）：
 * - 手机：SubPagePhone（LargeFlexibleTopAppBar），内容水平内边距 16，间距 12，视频 2 列 / 海报 3 列 / 列表；
 * - 平板：SubPageTabletHeader（返回 + PageTitleLarge），Adaptive(180)（海报 Adaptive(120)），间距 16；
 * - 电视：SubPageTvTitle（48, 27），Fixed(4) 视频 / Fixed(6) 海报，水平 48、底部 27，间距 18，
 *   focusRestorer + focusGroup，第一项初始焦点。
 */

/** 网格内容的种类。 */
internal enum class SubGridKind { Video, Poster, List }

/** 一个网格的列数、内边距与间距。tv = 电视（加 focusRestorer / focusGroup / TvColumnPivot）。 */
@Immutable
internal class SubGridSpec(
    val columns: GridCells,
    val contentPadding: PaddingValues,
    val spacing: Dp,
    val tv: Boolean,
)

/** 模板页三端的布局参数。horizontal：内容水平内边距；top / bottom：网格上下内边距。 */
@Immutable
internal class SubPageSpec(
    val layout: DeviceLayout,
    val horizontal: Dp,
    val top: Dp,
    val bottom: Dp,
    val spacing: Dp,
    private val wide: Boolean = false,
) {
    val tv: Boolean get() = layout == DeviceLayout.Tv

    fun columns(kind: SubGridKind): GridCells = when (layout) {
        DeviceLayout.Phone -> when (kind) {
            SubGridKind.Video -> GridCells.Fixed(if (wide) 3 else 2)
            SubGridKind.Poster -> GridCells.Fixed(if (wide) 4 else 3)
            SubGridKind.List -> GridCells.Fixed(if (wide) 2 else 1)
        }

        DeviceLayout.Tablet -> when (kind) {
            SubGridKind.Video -> GridCells.Adaptive(180.dp)
            SubGridKind.Poster -> GridCells.Adaptive(120.dp)
            SubGridKind.List -> GridCells.Adaptive(340.dp)
        }

        DeviceLayout.Tv -> when (kind) {
            SubGridKind.Video -> GridCells.Fixed(4)
            SubGridKind.Poster -> GridCells.Fixed(6)
            SubGridKind.List -> GridCells.Fixed(3)
        }
    }

    /** 列表行自带 16dp 内边距：手机列表水平只留 8（同设置页列表），行距收紧。 */
    fun grid(kind: SubGridKind, top: Dp = this.top): SubGridSpec {
        val list = kind == SubGridKind.List
        val h = if (list && layout == DeviceLayout.Phone) 8.dp else horizontal
        val gap = if (!list) spacing else when (layout) {
            DeviceLayout.Phone -> 2.dp
            DeviceLayout.Tablet -> 8.dp
            DeviceLayout.Tv -> 14.dp
        }
        return SubGridSpec(
            columns = columns(kind),
            contentPadding = PaddingValues(start = h, end = h, top = top, bottom = bottom),
            spacing = gap,
            tv = tv,
        )
    }
}

/**
 * 模板页框架：按设备画像分派三端的标题栏，content 收到 [SubPageSpec]（在一个铺满剩余空间的 Column 里）。
 * actions：手机顶栏右侧 / 平板标题行右侧 / 电视标题行右侧（靠右）。
 */
@Composable
internal fun SubPageFrame(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.(SubPageSpec) -> Unit,
) {
    val profile = LocalDeviceProfile.current
    when (profile.layout) {
        DeviceLayout.Phone -> SubPagePhone(title = title, onBack = onBack, actions = actions) { bottom ->
            val spec = SubPageSpec(
                layout = DeviceLayout.Phone,
                horizontal = 16.dp,
                top = 8.dp,
                bottom = bottom,
                spacing = 12.dp,
                wide = profile.width != WidthClass.Compact,
            )
            Column(Modifier.fillMaxSize()) { content(spec) }
        }

        DeviceLayout.Tablet -> Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SubPageTabletHeader(title = title, onBack = onBack, trailing = actions)
            val spec = SubPageSpec(
                layout = DeviceLayout.Tablet,
                horizontal = 24.dp,
                top = 4.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                spacing = 16.dp,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { content(spec) }
        }

        DeviceLayout.Tv -> Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SubPageTvTitle(title) {
                Spacer(Modifier.weight(1f))
                actions()
            }
            val spec = SubPageSpec(
                layout = DeviceLayout.Tv,
                horizontal = 48.dp,
                top = 16.dp,
                bottom = 27.dp,
                spacing = 18.dp,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { content(spec) }
        }
    }
}

/**
 * 模板网格：列表为空时整页加载 / 空提示（有 [header] 时空提示放在 header 下面）；快到底部时 [onLoadMore]；
 * 还有更多（!noMore）时末尾显示 LoadMoreIndicator。电视上第一项（[firstRequester]）为初始焦点。
 * itemContent 的 modifier 参数要加在卡片最外层（第一项带 focusRequester）。
 */
@Composable
internal fun <T> SubPageGrid(
    list: List<T>,
    grid: SubGridSpec,
    loading: Boolean,
    noMore: Boolean,
    modifier: Modifier = Modifier,
    onLoadMore: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    emptyMessage: String? = null,
    firstRequester: FocusRequester? = null,
    state: LazyGridState = rememberLazyGridState(),
    header: (LazyGridScope.() -> Unit)? = null,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T, modifier: Modifier) -> Unit,
) {
    state.LoadMoreEffect { if (list.isNotEmpty()) onLoadMore?.invoke() }

    // 电视：第一项出现后把焦点给它（只做一次）
    var entryPending by remember { mutableStateOf(true) }
    LaunchedEffect(list.isNotEmpty()) {
        if (!grid.tv || firstRequester == null || !entryPending || list.isEmpty()) return@LaunchedEffect
        withFrameNanos { }
        firstRequester.tryRequestFocus()
        entryPending = false
    }

    if (list.isEmpty() && header == null) {
        EmptyOrLoading(
            loading = loading,
            modifier = modifier.fillMaxSize(),
            message = emptyMessage,
            onRetry = onRetry,
        )
        return
    }

    TvColumnPivot {
        LazyVerticalGrid(
            columns = grid.columns,
            state = state,
            modifier = if (grid.tv) {
                modifier
                    .fillMaxSize()
                    .focusRestorer()
                    .focusGroup()
            } else {
                modifier.fillMaxSize()
            },
            contentPadding = grid.contentPadding,
            horizontalArrangement = Arrangement.spacedBy(grid.spacing),
            verticalArrangement = Arrangement.spacedBy(grid.spacing),
        ) {
            header?.invoke(this)
            if (list.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                    EmptyOrLoading(
                        loading = loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        message = emptyMessage,
                        onRetry = onRetry,
                    )
                }
            } else {
                items(
                    count = list.size,
                    key = itemKey?.let { keyOf -> { index: Int -> keyOf(list[index]) } },
                    contentType = { "item" },
                ) { index ->
                    val item = list[index]
                    itemContent(
                        index,
                        item,
                        if (index == 0 && firstRequester != null) Modifier.focusRequester(firstRequester) else Modifier,
                    )
                }
                if (!noMore) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }, contentType = "loading") {
                        LoadMoreIndicator()
                    }
                }
            }
        }
    }
}

/**
 * 部分旧 ViewModel 的「加载中」不可观察（私有、非状态）：调用加载后最多等 [timeoutMs]，期间按加载中显示，
 * 之后仍为空就显示空提示 + 重试。[retry] 重新计时。
 */
@Stable
internal class PendingLoad {
    var attempt by mutableIntStateOf(0)
        private set
    var waiting by mutableStateOf(true)
        internal set

    fun retry() {
        waiting = true
        attempt++
    }
}

@Composable
internal fun rememberPendingLoad(timeoutMs: Long = 12_000L): PendingLoad {
    val pending = remember { PendingLoad() }
    LaunchedEffect(pending.attempt) {
        pending.waiting = true
        delay(timeoutMs)
        pending.waiting = false
    }
    return pending
}

/** 未登录时的整页提示：说明文字 + 「登录」按钮（电视上按钮为初始焦点）。 */
@Composable
internal fun SubPageLoginPrompt(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    tv: Boolean = LocalDeviceProfile.current.isTv,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(tv) {
        if (!tv) return@LaunchedEffect
        withFrameNanos { }
        requester.tryRequestFocus()
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.me_login_hint),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            BvButton(
                text = stringResource(R.string.me_login),
                onClick = onLogin,
                modifier = Modifier.focusRequester(requester),
            )
        }
    }
}
