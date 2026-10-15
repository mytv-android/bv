package dev.aaa1115910.bv.ui.feature.video

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvListItem
import dev.aaa1115910.bv.ui.designsystem.focus.BvKey
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.focus.onBvKey
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.viewmodel.video.VideoActionsViewModel
import kotlinx.coroutines.launch

/** 对话框外框：圆角 28、surfaceContainerHigh、内边距 24。 */
@Composable
private fun DetailDialogFrame(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                Modifier
                    .widthIn(min = 280.dp, max = 520.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        }
    }
}

/** 投币对话框：选择 1 枚 / 2 枚，可同时点赞。 */
@Composable
internal fun CoinDialog(vm: VideoActionsViewModel, onDismiss: () -> Unit) {
    var alsoLike by remember { mutableStateOf(!vm.liked) }
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstRequester.requestFocus() } }
    DetailDialogFrame(onDismiss) {
        Text(stringResource(R.string.video_detail_coin_title), style = BvType.PanelTitle)
        if (vm.coinCount > 0) {
            Text(
                stringResource(R.string.video_detail_coin_sent, vm.coinCount),
                style = BvType.Small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BvButton(
                text = stringResource(R.string.video_detail_coin_one),
                onClick = {
                    vm.coin(1, alsoLike)
                    onDismiss()
                },
                modifier = Modifier.focusRequester(firstRequester),
                style = BvButtonStyle.Tonal,
            )
            if (vm.coinCount == 0) {
                BvButton(
                    text = stringResource(R.string.video_detail_coin_two),
                    onClick = {
                        vm.coin(2, alsoLike)
                        onDismiss()
                    },
                )
            }
        }
        if (!vm.liked) {
            BvChip(
                text = stringResource(R.string.video_detail_coin_also_like),
                selected = alsoLike,
                onClick = { alsoLike = !alsoLike },
                icon = if (alsoLike) Icons.Rounded.Done else null,
            )
        }
    }
}

/** 收藏夹多选对话框（照抄旧电视 FavoriteDialog：每次勾选立即提交）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FavoriteDialog(vm: VideoActionsViewModel, onDismiss: () -> Unit) {
    val doneRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { doneRequester.requestFocus() } }
    DetailDialogFrame(onDismiss) {
        Text(stringResource(R.string.video_detail_favorite_title), style = BvType.PanelTitle)
        when {
            vm.favoriteFoldersLoading && vm.favoriteFolders.isEmpty() -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) { LoadingIndicator() }

            vm.favoriteFolders.isEmpty() -> Text(
                stringResource(R.string.video_detail_favorite_empty),
                style = BvType.Small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> FlowRow(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.favoriteFolders.forEach { folder ->
                    val selected = folder.videoInThisFav
                    BvChip(
                        text = folder.title,
                        selected = selected,
                        icon = if (selected) Icons.Rounded.Done else null,
                        onClick = {
                            val ids = vm.favoriteFolders.filter { it.videoInThisFav }.map { it.id }
                            vm.updateFavoriteFolders(if (selected) ids - folder.id else ids + folder.id)
                        },
                    )
                }
            }
        }
        BvButton(
            text = stringResource(R.string.video_detail_done),
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.End)
                .focusRequester(doneRequester),
            style = BvButtonStyle.Tonal,
        )
    }
}

/** 分 P / 剧集抽屉（旧手机 VideoPlayerPages 的 ModalBottomSheet）：多行时顶部用标签切换。 */
@Composable
internal fun EpisodeSheet(
    rows: List<DetailEpisodeRow>,
    initialRowKey: String,
    currentCid: Long,
    onPlay: (DetailEpisodeRow, DetailEpisode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var rowKey by remember { mutableStateOf(initialRowKey) }
    val row = rows.firstOrNull { it.key == rowKey } ?: rows.firstOrNull() ?: return
    val listState = rememberLazyListState()
    LaunchedEffect(row.key) {
        val index = row.episodes.indexOfFirst { it.cid == currentCid }
        if (index > 2) runCatching { listState.scrollToItem(index - 2) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                row.title,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = BvType.HeroPhone,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (rows.size > 1) {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    itemsIndexed(rows, key = { _, r -> r.key }) { _, r ->
                        BvChip(
                            text = r.title,
                            selected = r.key == row.key,
                            onClick = { rowKey = r.key },
                        )
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                itemsIndexed(row.episodes, key = { index, ep -> "${ep.cid}-$index" }) { _, ep ->
                    val playing = ep.cid == currentCid
                    BvListItem(
                        title = ep.label,
                        selected = playing,
                        icon = if (playing) Icons.Filled.PlayArrow else null,
                        iconContainerColor = MaterialTheme.colorScheme.primary,
                        iconContentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            onPlay(row, ep)
                            scope.launch {
                                runCatching { sheetState.hide() }
                                onDismiss()
                            }
                        },
                        trailing = if (ep.durationMs > 0) {
                            {
                                Text(
                                    formatDuration(ep.durationMs),
                                    style = BvType.Label,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else null,
                    )
                }
                item(key = "nav") { Box(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

/** 内嵌播放器右上角「更多」：稍后再看、分享。 */
@Composable
internal fun DetailMoreSheet(
    vm: VideoActionsViewModel,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .navigationBarsPadding(),
        ) {
            BvListItem(
                title = stringResource(R.string.video_detail_to_view),
                icon = Icons.Outlined.WatchLater,
                selected = vm.inToView,
                onClick = {
                    vm.toggleToView()
                    onDismiss()
                },
            )
            BvListItem(
                title = stringResource(R.string.video_detail_share),
                icon = Icons.Outlined.Share,
                onClick = {
                    onShare()
                    onDismiss()
                },
            )
        }
    }
}

/** 电视简介全文对话框（旧电视 VideoDescriptionDialog：上下键滚动）。 */
@Composable
internal fun TvDescriptionDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.video_detail_description), style = BvType.PanelTitle)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .focusRequester(requester)
                        .onBvKey { key ->
                            when (key) {
                                BvKey.Up -> {
                                    scope.launch { listState.animateScrollBy(-listState.layoutInfo.viewportSize.height / 3f) }
                                    true
                                }

                                BvKey.Down -> {
                                    scope.launch { listState.animateScrollBy(listState.layoutInfo.viewportSize.height / 3f) }
                                    true
                                }

                                else -> false
                            }
                        }
                        .bvFocus(scale = 1f, corner = 16.dp)
                        .focusable(),
                ) {
                    item {
                        Text(title, style = BvType.BodyBold)
                    }
                    item {
                        Text(
                            description,
                            modifier = Modifier.padding(top = 8.dp),
                            style = TvDescriptionStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
