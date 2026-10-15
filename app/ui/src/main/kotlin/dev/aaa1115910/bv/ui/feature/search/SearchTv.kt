package dev.aaa1115910.bv.ui.feature.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvListItem
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.focus.BvKey
import dev.aaa1115910.bv.ui.designsystem.focus.TvRowPivot
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.focus.focusContainerColor
import dev.aaa1115910.bv.ui.designsystem.focus.focusContentColor
import dev.aaa1115910.bv.ui.designsystem.focus.onBvKey
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import kotlinx.coroutines.delay

/** 屏幕键盘：A–Z、1–9、0，6 列。 */
private val TvKeys = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".map { it.toString() }

/** 中列（联想 / 历史）与右列（热搜）最多显示的条数。 */
private const val TvColumnItems = 6

private enum class TvFocusArea { Keyboard, Results }

/** requestFocus 不抛异常且成功时返回 true。 */
private fun FocusRequester.tryFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/**
 * 电视搜索（原型 searchTv）：外壳在左侧叠 TvRail（选中「搜索」），内容从 x = 96 开始。
 * - 左列 (96, 27) 宽 300：搜索框（52 高，已输入字母 + primary 光标）、6 列键盘（初始焦点「A」）、空格 / 删除 / 清空、语音按钮；
 * - 中列 (430, 27) 宽 190：联想（无输入时显示搜索历史）；右列 (650, 27) 宽 262：热搜；
 * - 结果行 (430, 330)：输入变化 500ms 防抖后刷新；↓ 进入结果行；菜单键打开「完整结果」页（4 列网格 + 顶部分类标签）。
 */
@Composable
internal fun SearchTv(nav: NavController, state: SearchUiState, follow: FollowState, initialFull: Boolean) {
    var full by rememberSaveable { mutableStateOf(initialFull) }
    var lastArea by rememberSaveable { mutableStateOf(TvFocusArea.Keyboard) }
    var showFilter by remember { mutableStateOf(false) }
    val openers = rememberSearchOpeners(nav, state)
    val keyword = state.input.keyword

    SuggestEffect(state)
    // 输入变化 500ms 防抖后刷新结果（拼音首字母直接作为关键词，B 站接口支持拼音联想）
    LaunchedEffect(keyword) {
        val kw = keyword.trim()
        if (kw.isEmpty()) return@LaunchedEffect
        delay(500)
        if (kw != state.submitted) state.refresh(kw)
    }

    BackHandler(enabled = full) { full = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onBvKey { key ->
                if (key == BvKey.Menu && !full && state.submitted.isNotEmpty() && state.input.keyword.isNotBlank()) {
                    state.input.addSearchHistory(state.submitted)
                    full = true
                    true
                } else {
                    false
                }
            }
    ) {
        if (full) {
            SearchTvFull(state, follow, openers, onFilter = { showFilter = true })
        } else {
            SearchTvInput(
                state = state,
                openers = openers,
                lastArea = lastArea,
                onArea = { lastArea = it },
            )
        }
    }
    if (showFilter) SearchFilterDialog(state, onDismiss = { showFilter = false })
}

@Composable
private fun SearchTvInput(
    state: SearchUiState,
    openers: SearchOpeners,
    lastArea: TvFocusArea,
    onArea: (TvFocusArea) -> Unit,
) {
    val context = LocalContext.current
    val keyword = state.input.keyword
    val keyRequester = remember { FocusRequester() }
    val resultsRequester = remember { FocusRequester() }
    val cards = state.videoCards()
    val rowState = rememberLazyListState()
    val showResults = keyword.isNotBlank() && state.submitted.isNotEmpty()

    val voiceAvailable = remember(context) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.packageManager) != null
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                state.input.keyword = it
            }
        }
    }
    val pick: (String) -> Unit = { word ->
        state.input.keyword = word
        state.input.addSearchHistory(word)
        state.refresh(word)
    }

    // 左列最下面一行按 ↓ 直接进入结果行（结果行在右下方，不在默认的焦点搜索方向上）
    val downToResults = Modifier.focusProperties {
        if (showResults && cards.isNotEmpty()) down = resultsRequester
    }

    // 初始焦点「A」；从详情页 / 完整结果页返回时回到结果行
    LaunchedEffect(Unit) {
        withFrameNanos { }
        val restored = lastArea == TvFocusArea.Results && showResults && cards.isNotEmpty() && resultsRequester.tryFocus()
        if (!restored) keyRequester.tryFocus()
    }

    // ---------- 左列：搜索框 + 键盘 + 语音 ----------
    Column(
        Modifier
            .padding(start = 96.dp, top = 27.dp)
            .width(300.dp)
            .onFocusChanged { if (it.hasFocus) onArea(TvFocusArea.Keyboard) },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TvQueryField(keyword)
        Column(
            Modifier
                .focusRestorer()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TvKeys.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        TvKey(
                            text = key,
                            onClick = { state.input.keyword = state.input.keyword + key },
                            modifier = Modifier
                                .weight(1f)
                                .then(if (key == "A") Modifier.focusRequester(keyRequester) else Modifier),
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TvKey(
                text = stringResource(R.string.search_tv_space),
                onClick = { if (state.input.keyword.isNotEmpty()) state.input.keyword = state.input.keyword + " " },
                modifier = Modifier
                    .weight(1f)
                    .then(if (voiceAvailable) Modifier else downToResults),
            )
            TvKey(
                text = stringResource(R.string.search_tv_delete),
                onClick = { state.input.keyword = state.input.keyword.dropLast(1) },
                modifier = Modifier
                    .weight(1f)
                    .then(if (voiceAvailable) Modifier else downToResults),
            )
            TvKey(
                text = stringResource(R.string.search_tv_clear),
                onClick = { state.input.keyword = "" },
                modifier = Modifier
                    .weight(1f)
                    .then(if (voiceAvailable) Modifier else downToResults),
            )
        }
        if (voiceAvailable) {
            TvWideButton(
                icon = Icons.Rounded.Mic,
                text = stringResource(R.string.search_tv_voice),
                modifier = downToResults,
                onClick = {
                    runCatching {
                        voiceLauncher.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        )
                    }
                },
            )
        }
    }

    // ---------- 中列：联想（无输入时为搜索历史） ----------
    val showHistory = keyword.isBlank()
    val middleItems = if (showHistory) {
        state.input.searchHistories.map { it.keyword }
    } else {
        (state.input.matchedSearchHistories.map { it.keyword } + state.input.suggests).distinct()
    }.take(TvColumnItems)
    Column(
        Modifier
            .padding(start = 430.dp, top = 27.dp)
            .width(190.dp)
            .focusRestorer()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(if (showHistory) R.string.search_tv_history else R.string.search_tv_suggest),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            style = BvType.Eyebrow,
            color = MaterialTheme.colorScheme.primary,
        )
        middleItems.forEach { word ->
            BvListItem(
                title = word,
                onClick = { pick(word) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                titleStyle = BvType.CardTitle,
            )
        }
    }

    // ---------- 右列：热搜 ----------
    Column(
        Modifier
            .padding(start = 650.dp, top = 27.dp)
            .width(262.dp)
            .focusRestorer()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.search_tv_hot),
            style = BvType.Eyebrow,
            color = MaterialTheme.colorScheme.primary,
        )
        state.input.hotwords.take(TvColumnItems).forEachIndexed { index, word ->
            HotwordItem(
                rank = index + 1,
                text = word.showName.ifBlank { word.keyword },
                onClick = { pick(word.keyword) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // ---------- 结果行 (430, 330) ----------
    if (showResults) {
        Column(
            Modifier
                .padding(start = 430.dp, top = 330.dp)
                .fillMaxWidth()
        ) {
            SectionHeader(
                title = stringResource(R.string.search_results_for, state.submitted),
                modifier = Modifier.padding(start = 4.dp, end = 48.dp),
            )
            when {
                cards.isEmpty() && state.isLoading(SearchType.Video) -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center,
                ) { LoadingIndicator() }

                cards.isEmpty() -> Text(
                    stringResource(R.string.search_no_result),
                    modifier = Modifier.padding(start = 4.dp, top = 24.dp),
                    style = BvType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> {
                    rowState.OnNearEnd { state.loadMore(SearchType.Video) }
                    TvRowPivot {
                        LazyRow(
                            state = rowState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(resultsRequester)
                                .focusRestorer()
                                .focusGroup()
                                .onFocusChanged { if (it.hasFocus) onArea(TvFocusArea.Results) },
                            contentPadding = PaddingValues(start = 4.dp, top = 12.dp, end = 48.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(cards.size, contentType = { "video" }) { index ->
                                val card = cards[index]
                                VideoCard(
                                    data = card,
                                    onClick = { openers.video(card) },
                                    width = 180.dp,
                                    showMeta = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 「完整结果」页（菜单键打开）：顶部分类标签（聚焦即选中，初始焦点在当前分类）+ 平板布局的电视版网格。
 */
@Composable
private fun SearchTvFull(
    state: SearchUiState,
    follow: FollowState,
    openers: SearchOpeners,
    onFilter: () -> Unit,
) {
    val chipRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        chipRequester.tryFocus()
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 96.dp, end = 48.dp, top = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchCategoryChips(
                selected = state.category,
                onSelect = { state.category = it },
                selectOnFocus = true,
                selectedRequester = chipRequester,
            )
            Text(
                stringResource(R.string.search_results_for, state.submitted),
                modifier = Modifier.weight(1f),
                style = BvType.Section,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SearchResultsPane(
            state = state,
            follow = follow,
            openers = openers,
            tv = true,
            onFilter = onFilter,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 96.dp, end = 48.dp, top = 16.dp, bottom = 27.dp),
        )
    }
}

/** 电视搜索框（原型 `.m-search` 高 52，surfaceContainerHighest）：已输入字母 16sp + primary 光标「|」。不可聚焦。 */
@Composable
private fun TvQueryField(keyword: String) {
    val scheme = MaterialTheme.colorScheme
    val placeholder = stringResource(R.string.search_tv_placeholder)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        color = scheme.surfaceContainerHighest,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = scheme.onSurfaceVariant,
            )
            Text(
                buildAnnotatedString {
                    append(keyword)
                    withStyle(SpanStyle(color = scheme.primary)) { append("|") }
                    if (keyword.isEmpty()) {
                        withStyle(SpanStyle(color = scheme.onSurfaceVariant)) { append(placeholder) }
                    }
                },
                modifier = Modifier.weight(1f),
                style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 键盘键（原型 `.m-key`）：高 34，圆角 10，surfaceContainerHigh，14sp SemiBold 等宽；聚焦放大 + 描边 + inverseSurface。 */
@Composable
private fun TvKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val container by focusContainerColor(focused, MaterialTheme.colorScheme.surfaceContainerHigh)
    val content by focusContentColor(focused, MaterialTheme.colorScheme.onSurface)
    Surface(
        onClick = onClick,
        modifier = modifier
            .bvFocus(corner = 10.dp)
            .height(34.dp),
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content,
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
            )
        }
    }
}

/** 语音按钮（原型 `.m-btn.t`，内容左对齐）：高 40，全圆角，secondaryContainer，Mic 20 + 14sp SemiBold。 */
@Composable
private fun TvWideButton(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val container by focusContainerColor(focused, MaterialTheme.colorScheme.secondaryContainer)
    val content by focusContentColor(focused, MaterialTheme.colorScheme.onSecondaryContainer)
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .bvFocus()
            .height(40.dp),
        shape = CircleShape,
        color = container,
        contentColor = content,
        interactionSource = interaction,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = BvType.Button, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
