package dev.aaa1115910.bv.ui.feature.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.RankBadge
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.PartitionUtil
import kotlinx.coroutines.launch

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
        items(
            items = histories,
            key = { "h$it" },
            contentType = { "history" },
        ) { text ->
            SuggestionRow(text, keyword, Icons.Rounded.History) { onPick(text) }
        }
        items(
            items = suggests,
            key = { "s$it" },
            contentType = { "suggest" },
        ) { text ->
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

/** 一组筛选：标题 + 自动换行的 chip；[enabled] = false 时整组置灰（chip 本身也要传 enabled = false）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, enabled: Boolean = true, content: @Composable () -> Unit) {
    Column(
        Modifier.alpha(if (enabled) 1f else DisabledAlpha),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

/** 手机筛选抽屉收起时显示的分区个数（不含「全部分区」；已选的分区总会显示）。 */
private const val CollapsedPartitions = 7

/** 不可用的筛选组的透明度（M3 disabled 内容 38%）。 */
private const val DisabledAlpha = 0.38f

/**
 * 筛选的四组单选 chip：排序 / 时长 / 分区 / 子分区（选了分区才出现），选中即生效并重新搜索。
 * App 接口只有排序（选项也不同），时长和分区置灰并提示去设置里切到 Web 接口。
 * [collapsePartitions] = true 时（手机抽屉）分区先只显示一部分，末尾一个 chip 展开 / 收起。
 */
@Composable
private fun SearchFilterGroups(
    state: SearchUiState,
    firstRequester: FocusRequester? = null,
    collapsePartitions: Boolean = false,
) {
    val result = state.result
    val full = state.fullFilters
    var expanded by remember { mutableStateOf(!collapsePartitions) }
    FilterGroup(stringResource(R.string.search_filter_order)) {
        state.orderFilters.forEach { order ->
            val selected = order == result.selectedOrder
            BvChip(
                text = stringResource(order.labelRes()),
                selected = selected,
                onClick = { state.setOrder(order) },
                modifier = if (selected && firstRequester != null) Modifier.focusRequester(firstRequester) else Modifier,
                icon = if (selected) Icons.Rounded.Check else null,
            )
        }
    }
    if (!full) {
        Text(
            stringResource(R.string.search_filter_app_hint),
            style = BvType.Meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FilterGroup(stringResource(R.string.search_filter_duration), enabled = full) {
        SearchFilterDuration.entries.forEach { duration ->
            val selected = duration == result.selectedDuration
            BvChip(
                text = stringResource(duration.labelRes()),
                selected = selected,
                onClick = { state.setDuration(duration) },
                icon = if (selected) Icons.Rounded.Check else null,
                enabled = full,
            )
        }
    }
    FilterGroup(stringResource(R.string.search_filter_partition), enabled = full) {
        val all = PartitionUtil.partitions
        val selectedPartition = result.selectedPartition
        val shown = if (expanded) {
            all
        } else {
            all.take(CollapsedPartitions).let { if (selectedPartition != null && selectedPartition !in it) it + selectedPartition else it }
        }
        BvChip(
            text = stringResource(R.string.search_partition_all),
            selected = selectedPartition == null,
            onClick = { state.setPartition(null) },
            icon = if (selectedPartition == null) Icons.Rounded.Check else null,
            enabled = full,
        )
        shown.forEach { partition ->
            val selected = partition == selectedPartition
            BvChip(
                text = partition.strRes,
                selected = selected,
                onClick = { state.setPartition(partition) },
                icon = if (selected) Icons.Rounded.Check else null,
                enabled = full,
            )
        }
        if (collapsePartitions) {
            BvChip(
                text = if (expanded) {
                    stringResource(R.string.search_filter_collapse_partitions)
                } else {
                    stringResource(R.string.search_filter_expand_partitions, all.size)
                },
                selected = false,
                onClick = { expanded = !expanded },
                enabled = full,
                trailing = {
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
    val children = result.selectedPartition?.children.orEmpty()
    if (children.isNotEmpty()) {
        FilterGroup(stringResource(R.string.search_filter_child_partition)) {
            children.forEach { child ->
                val selected = child == result.selectedChildPartition
                BvChip(
                    text = child.strRes,
                    selected = selected,
                    onClick = { state.setChildPartition(child) },
                    icon = if (selected) Icons.Rounded.Check else null,
                )
            }
        }
    }
}

/**
 * 视频结果筛选（照抄旧电视版 SearchResultVideoFilter：排序 / 时长 / 分区 / 子分区，选中即生效并重新搜索）。
 * 平板 / 电视用这个对话框，手机用 [SearchFilterSheet]。App 接口下只能排序。
 */
@Composable
internal fun SearchFilterDialog(state: SearchUiState, onDismiss: () -> Unit) {
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstRequester.requestFocus() } }
    SearchDialogFrame(onDismiss) {
        Text(stringResource(R.string.search_filter_title), style = BvType.PanelTitle)
        SearchFilterGroups(state, firstRequester)
        BvButton(
            text = stringResource(R.string.search_filter_done),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            style = BvButtonStyle.Tonal,
        )
    }
}

/** 筛选条上的下拉 chip：当前值 + ▾，改过（非默认值）时高亮。 */
@Composable
private fun FilterDropChip(text: String, changed: Boolean, onClick: () -> Unit) {
    BvChip(
        text = text,
        selected = changed,
        onClick = onClick,
        contentPadding = PaddingValues(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        trailing = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

/**
 * 手机结果上方的筛选条（原型 filterBar）：「筛选」+ 已改项数角标，然后是排序 / 时长 / 分区三个下拉 chip。
 * 点任意一个都打开 [SearchFilterSheet]。
 */
@Composable
internal fun SearchFilterBar(state: SearchUiState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val result = state.result
    val count = state.filterCount
    val duration = result.selectedDuration
    val partition = result.selectedPartition
    val child = result.selectedChildPartition
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvChip(
            text = stringResource(R.string.search_filter_title),
            selected = false,
            onClick = onOpen,
            icon = Icons.Rounded.Tune,
            contentPadding = PaddingValues(start = 10.dp, end = if (count > 0) 8.dp else 14.dp, top = 6.dp, bottom = 6.dp),
            trailing = if (count > 0) {
                {
                    Text(
                        count.toString(),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .widthIn(min = 18.dp)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        style = BvType.Badge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                null
            },
        )
        FilterDropChip(
            text = stringResource(result.selectedOrder.labelRes()),
            changed = result.selectedOrder != DefaultSearchOrder,
            onClick = onOpen,
        )
        // App 接口只能排序，时长和分区的 chip 不放在筛选条上（抽屉里置灰）
        if (state.fullFilters) {
            FilterDropChip(
                text = stringResource(if (duration == SearchFilterDuration.All) R.string.search_filter_duration else duration.labelRes()),
                changed = duration != SearchFilterDuration.All,
                onClick = onOpen,
            )
            FilterDropChip(
                text = when {
                    partition == null -> stringResource(R.string.search_filter_partition)
                    child == null -> partition.strRes
                    else -> stringResource(R.string.search_filter_partition_child, partition.strRes, child.strRes)
                },
                changed = partition != null,
                onClick = onOpen,
            )
        }
    }
}

/**
 * 手机筛选抽屉（原型 searchFilterPhone）：ModalBottomSheet，标题「筛选视频」+「重置」，说明一行，
 * 四组单选 chip（分区默认收起一部分），底部「完成」。选中即生效，抽屉后面的结果同步刷新。
 */
@Composable
internal fun SearchFilterSheet(state: SearchUiState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.search_filter_sheet_title),
                        modifier = Modifier.weight(1f),
                        style = BvType.PanelTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (state.filterCount > 0) {
                        Text(
                            stringResource(R.string.search_filter_reset),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = state::resetFilters)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            style = BvType.BodyBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    stringResource(R.string.search_filter_hint),
                    style = BvType.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SearchFilterGroups(state, collapsePartitions = true)
            BvButton(
                text = stringResource(R.string.search_filter_done),
                onClick = {
                    scope.launch {
                        runCatching { sheetState.hide() }
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                large = true,
            )
        }
    }
}
