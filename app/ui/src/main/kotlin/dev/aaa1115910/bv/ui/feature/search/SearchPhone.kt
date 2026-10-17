package dev.aaa1115910.bv.ui.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvSearchField
import kotlin.math.roundToInt

/**
 * 手机搜索（原型 searchPhone / searchResultPhone）：
 * 顶栏 Row(padding(top = statusBar + 6, start 12, end 12, bottom 8), spacedBy 6)：返回 + 搜索框（自动弹键盘）；
 * 有输入 → 联想；无输入 → 历史 + 热搜；提交后 → 顶栏下加分类标签行 + 筛选条（综合 / 视频）+ 结果列表，
 * 往下滚时整个顶栏收起（只留状态栏），往上滚立刻露出。点筛选条打开筛选抽屉。
 */
@Composable
internal fun SearchPhone(nav: NavController, state: SearchUiState, follow: FollowState) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val openers = rememberSearchOpeners(nav, state)
    val keyword = state.input.keyword
    val hasResults = state.submitted.isNotEmpty()
    val showResults = !state.editing && hasResults
    var showFilter by remember { mutableStateOf(false) }

    val submit: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            keyboard?.hide()
            state.search(query)
        }
    }
    // 在结果页重新编辑时，返回键回到结果；否则离开搜索页
    val closeEditing: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        state.input.keyword = state.submitted
        state.editing = false
    }
    val back: () -> Unit = {
        if (state.editing && hasResults) {
            closeEditing()
        } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            nav.popBackStack()
        }
    }
    BackHandler(enabled = state.editing && hasResults) { closeEditing() }
    SuggestEffect(state)

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val header = remember { CollapsingHeader() }
    SideEffect { header.enabled = showResults }
    // 新的结果列表从顶部开始（换关键词 / 分类 / 筛选、回到编辑），顶栏要完全展开，否则顶部会空一截
    val result = state.result
    LaunchedEffect(
        showResults, state.submitted, state.category,
        result.selectedOrder, result.selectedDuration, result.selectedPartition, result.selectedChildPartition,
    ) {
        header.offsetPx = 0f
    }
    val contentTop = statusTop + with(LocalDensity.current) { header.heightPx.toDp() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(header.connection)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            when {
                // 结果从顶栏下面滚过去，顶栏收起后结果占满整屏
                showResults -> SearchResultsList(
                    state = state,
                    follow = follow,
                    openers = openers,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentTop + 4.dp, bottom = 16.dp + navBottom),
                )

                keyword.isNotBlank() -> SuggestionList(
                    state = state,
                    onPick = submit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = contentTop),
                    contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = navBottom),
                )

                else -> SearchIdleContent(
                    state = state,
                    onPick = submit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = contentTop),
                )
            }
        }
        // 顶栏盖在内容上：搜索框，结果页再加分类和筛选条
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .onSizeChanged { header.heightPx = it.height }
                .offset { IntOffset(0, header.offsetPx.roundToInt()) }
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BvIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.search_back),
                    onClick = back,
                )
                BvSearchField(
                    value = keyword,
                    placeholder = stringResource(R.string.search_placeholder),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { if (it.hasFocus) state.editing = true },
                    onValueChange = state::onInput,
                    onSearch = submit,
                    onVoiceResult = submit,
                    active = true,
                    autoFocus = !hasResults,
                )
            }
            if (showResults) {
                SearchCategoryChips(
                    selected = state.category,
                    onSelect = { state.category = it },
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
                // 筛选只作用于视频，所以只在「综合」和「视频」显示
                if (state.category == SearchCategory.All || state.category == SearchCategory.Video) {
                    SearchFilterBar(
                        state = state,
                        onOpen = { showFilter = true },
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
            }
        }
        // 状态栏底色：顶栏收起时从它下面滑走，状态栏区域不露出内容
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
    if (showFilter && showResults) SearchFilterSheet(state, onDismiss = { showFilter = false })
}

/**
 * 结果页顶栏的 enterAlways 收起（原型 searchResultPhone）：顶栏位移 = 结果列表实际滚动的距离，
 * 往下滚收起、往上滚立刻露出。只跟随列表真正消耗的距离，所以列表回到顶部时顶栏一定完全展开。
 */
@Stable
private class CollapsingHeader {
    var heightPx by mutableIntStateOf(0)
    var offsetPx by mutableFloatStateOf(0f)

    /** 只在结果页生效；联想 / 历史页滚动时顶栏不动。 */
    var enabled = false

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (enabled) offsetPx = (offsetPx + consumed.y).coerceIn(-heightPx.toFloat(), 0f)
            return Offset.Zero
        }
    }
}
