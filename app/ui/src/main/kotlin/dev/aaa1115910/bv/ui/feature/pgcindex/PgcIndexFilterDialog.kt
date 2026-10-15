package dev.aaa1115910.bv.ui.feature.pgcindex

import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.aaa1115910.biliapi.entity.pgc.index.Area
import dev.aaa1115910.biliapi.entity.pgc.index.Copyright
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrder
import dev.aaa1115910.biliapi.entity.pgc.index.IndexOrderType
import dev.aaa1115910.biliapi.entity.pgc.index.IsFinish
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexParam
import dev.aaa1115910.biliapi.entity.pgc.index.Producer
import dev.aaa1115910.biliapi.entity.pgc.index.ReleaseDate
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonMonth
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonStatus
import dev.aaa1115910.biliapi.entity.pgc.index.SeasonVersion
import dev.aaa1115910.biliapi.entity.pgc.index.SpokenLanguage
import dev.aaa1115910.biliapi.entity.pgc.index.Style
import dev.aaa1115910.biliapi.entity.pgc.index.Year
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.timeline.tryRequestFocus
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.viewmodel.index.PgcIndexViewModel

/**
 * 索引筛选对话框（旧电视 component/pgc/IndexFilter）：排序方式、排序顺序，以及该类型支持的各项筛选，
 * 每项一行 BvChip（单选，选中项带 ✓）。改动立即生效（索引页会重新加载）。
 */
@Composable
internal fun PgcIndexFilterDialog(vm: PgcIndexViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val type = vm.pgcType
    val isTv = LocalDeviceProfile.current.isTv
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    val firstRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // 电视：打开时聚焦第一行（排序方式）的选中项；对话框窗口可能要几帧后才拿到焦点
        LaunchedEffect(Unit) {
            if (!isTv) return@LaunchedEffect
            repeat(10) {
                withFrameNanos { }
                if (firstRequester.tryRequestFocus()) return@LaunchedEffect
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isTv) 0.8f else 0.92f)
                .widthIn(max = 760.dp)
                .heightIn(max = maxHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier.padding(top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.pgcindex_filter_title, type.getDisplayName(context)),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = BvType.PanelTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    filterRow(
                        R.string.pgcindex_order, IndexOrder.getList(type), vm.indexOrder,
                        onSelect = { vm.indexOrder = it }, requester = firstRequester,
                    )
                    filterRow(
                        R.string.pgcindex_order_type, IndexOrderType.entries, vm.indexOrderType,
                        onSelect = { vm.indexOrderType = it },
                    )
                    filterRow(
                        R.string.pgcindex_season_version, SeasonVersion.getList(type), vm.seasonVersion,
                        onSelect = { vm.seasonVersion = it },
                    )
                    filterRow(
                        R.string.pgcindex_spoken_language, SpokenLanguage.getList(type), vm.spokenLanguage,
                        onSelect = { vm.spokenLanguage = it },
                    )
                    filterRow(
                        R.string.pgcindex_is_finish, IsFinish.getList(type), vm.isFinish,
                        onSelect = { vm.isFinish = it },
                    )
                    filterRow(
                        R.string.pgcindex_season_status, SeasonStatus.getList(type), vm.seasonStatus,
                        onSelect = { vm.seasonStatus = it },
                    )
                    filterRow(
                        R.string.pgcindex_area, Area.getList(type), vm.area,
                        onSelect = { vm.area = it },
                    )
                    filterRow(
                        R.string.pgcindex_copyright, Copyright.getList(type), vm.copyright,
                        onSelect = { vm.copyright = it },
                    )
                    filterRow(
                        R.string.pgcindex_season_month, SeasonMonth.getList(type), vm.seasonMonth,
                        onSelect = { vm.seasonMonth = it },
                    )
                    filterRow(
                        R.string.pgcindex_producer, Producer.getList(type), vm.producer,
                        onSelect = { vm.producer = it },
                    )
                    filterRow(
                        R.string.pgcindex_year, Year.getList(type), vm.year,
                        onSelect = { vm.year = it },
                    )
                    filterRow(
                        R.string.pgcindex_release_date, ReleaseDate.getList(type), vm.releaseDate,
                        onSelect = { vm.releaseDate = it },
                    )
                    filterRow(
                        R.string.pgcindex_style, Style.getList(type), vm.style,
                        onSelect = { vm.style = it },
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BvButton(text = stringResource(R.string.pgcindex_filter_done), onClick = onDismiss)
                }
            }
        }
    }
}

/** 一项筛选（没有可选项时不显示，同旧 indexFilterChipRow）。 */
private fun <T : PgcIndexParam> LazyListScope.filterRow(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    requester: FocusRequester? = null,
) {
    if (options.isEmpty()) return
    item(key = title) {
        FilterChipRow(
            title = stringResource(title),
            options = options,
            selected = selected,
            onSelect = onSelect,
            requester = requester,
        )
    }
}

@Composable
private fun <T : PgcIndexParam> FilterChipRow(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    requester: FocusRequester?,
) {
    val context = LocalContext.current
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = BvType.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            state = rowState,
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer()
                .focusGroup(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(options) { index, option ->
                val isSelected = option == selected
                BvChip(
                    text = option.getDisplayName(context),
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    modifier = if (requester != null && index == selectedIndex) Modifier.focusRequester(requester) else Modifier,
                    icon = if (isSelected) Icons.Filled.Check else null,
                )
            }
        }
    }
}
