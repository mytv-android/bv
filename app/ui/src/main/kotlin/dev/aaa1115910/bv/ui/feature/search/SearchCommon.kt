package dev.aaa1115910.bv.ui.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.RankBadge
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.PartitionUtil

/** 「大家都在搜」条数（手机 / 平板）。 */
private const val IdleHotCount = 10

/** 把与输入匹配的部分加粗（原型 searchPhone 联想列表）。 */
private fun highlight(text: String, keyword: String): AnnotatedString = buildAnnotatedString {
    val k = keyword.trim()
    val start = if (k.isEmpty()) -1 else text.indexOf(k, ignoreCase = true)
    if (start < 0) {
        append(text)
    } else {
        append(text.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(start, start + k.length)) }
        append(text.substring(start + k.length))
    }
}

/** 联想项（原型 `.m-li` padding 10×16）：搜索图标 + 文字（匹配部分加粗）。 */
@Composable
private fun SuggestionRow(text: String, keyword: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .bvFocus(corner = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                highlight(text, keyword),
                modifier = Modifier.weight(1f),
                style = BvType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 有输入时的联想列表：先列出匹配的搜索历史（沿用旧手机版），再列出接口联想。 */
@Composable
internal fun SuggestionList(
    state: SearchUiState,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp),
) {
    val keyword = state.input.keyword
    val histories = state.input.matchedSearchHistories.map { it.keyword }.distinct()
    val suggests = state.input.suggests.filter { it !in histories }.distinct()
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        items(histories, key = { "h$it" }) { text ->
            SuggestionRow(text, keyword, Icons.Rounded.History) { onPick(text) }
        }
        items(suggests, key = { "s$it" }) { text ->
            SuggestionRow(text, keyword, Icons.Rounded.Search) { onPick(text) }
        }
    }
}

/** 热搜行：RankBadge + 热搜词 13sp 1 行（原型 hotList）。 */
@Composable
internal fun HotwordItem(rank: Int, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .bvFocus(scale = 1.05f, corner = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank)
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = BvType.Small,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 无输入时（原型 searchPhone）：Column(padding 12×16, spacedBy 12)：
 * 「搜索历史」+「清除」、历史词 BvChip、「大家都在搜」、2 列热搜（水平 12、垂直 14）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchIdleContent(state: SearchUiState, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var confirmClear by remember { mutableStateOf(false) }
    val histories = state.input.searchHistories
    val hot = state.input.hotwords
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (histories.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.search_history),
                action = stringResource(R.string.search_history_clear),
                onAction = { confirmClear = true },
            )
            FlowRow(
                Modifier
                    .focusRestorer()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                histories.forEach { history ->
                    BvChip(text = history.keyword, selected = false, onClick = { onPick(history.keyword) })
                }
            }
        }
        if (hot.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.search_hot),
                modifier = Modifier.padding(top = if (histories.isNotEmpty()) 6.dp else 0.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                hot.take(IdleHotCount).chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEachIndexed { column, word ->
                            HotwordItem(
                                rank = row * 2 + column + 1,
                                text = word.showName.ifBlank { word.keyword },
                                onClick = { onPick(word.keyword) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if (confirmClear) {
        SearchDialogFrame(onDismiss = { confirmClear = false }) {
            Text(stringResource(R.string.search_history_clear_title), style = BvType.PanelTitle)
            Text(
                stringResource(R.string.search_history_clear_message),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier
                    .align(Alignment.End)
                    .focusRestorer()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BvButton(
                    text = stringResource(R.string.search_cancel),
                    onClick = { confirmClear = false },
                    style = BvButtonStyle.Outlined,
                )
                BvButton(
                    text = stringResource(R.string.search_history_clear_confirm),
                    onClick = {
                        confirmClear = false
                        state.input.deleteAllSearchHistories()
                    },
                )
            }
        }
    }
}

/** 对话框外框：圆角 28、surfaceContainerHigh、内边距 24（与视频详情的对话框一致）。 */
@Composable
internal fun SearchDialogFrame(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                Modifier
                    .widthIn(min = 280.dp, max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = BvType.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            Modifier
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

/**
 * 视频结果筛选（照抄旧电视版 SearchResultVideoFilter：排序 / 时长 / 分区 / 子分区，选中即生效并重新搜索）。
 * 只在 Web 接口下提供（旧版同样只在 Web 下打开）。
 */
@Composable
internal fun SearchFilterDialog(state: SearchUiState, onDismiss: () -> Unit) {
    val result = state.result
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstRequester.requestFocus() } }
    SearchDialogFrame(onDismiss) {
        Text(stringResource(R.string.search_filter_title), style = BvType.PanelTitle)
        FilterGroup(stringResource(R.string.search_filter_order)) {
            SearchFilterOrderType.webFilters.forEach { order ->
                val selected = order == result.selectedOrder
                BvChip(
                    text = stringResource(order.labelRes()),
                    selected = selected,
                    onClick = { state.setOrder(order) },
                    modifier = if (selected) Modifier.focusRequester(firstRequester) else Modifier,
                )
            }
        }
        FilterGroup(stringResource(R.string.search_filter_duration)) {
            SearchFilterDuration.entries.forEach { duration ->
                BvChip(
                    text = stringResource(duration.labelRes()),
                    selected = duration == result.selectedDuration,
                    onClick = { state.setDuration(duration) },
                )
            }
        }
        FilterGroup(stringResource(R.string.search_filter_partition)) {
            BvChip(
                text = stringResource(R.string.search_partition_all),
                selected = result.selectedPartition == null,
                onClick = { state.setPartition(null) },
            )
            PartitionUtil.partitions.forEach { partition ->
                BvChip(
                    text = partition.strRes,
                    selected = partition == result.selectedPartition,
                    onClick = { state.setPartition(partition) },
                )
            }
        }
        val children = result.selectedPartition?.children.orEmpty()
        if (children.isNotEmpty()) {
            FilterGroup(stringResource(R.string.search_filter_child_partition)) {
                children.forEach { child ->
                    BvChip(
                        text = child.strRes,
                        selected = child == result.selectedChildPartition,
                        onClick = { state.setChildPartition(child) },
                    )
                }
            }
        }
        BvButton(
            text = stringResource(R.string.search_filter_done),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            style = BvButtonStyle.Tonal,
        )
    }
}
