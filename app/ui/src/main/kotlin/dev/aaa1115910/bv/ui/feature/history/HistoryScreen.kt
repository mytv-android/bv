package dev.aaa1115910.bv.ui.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.adaptive.BvInputMode
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.feature.home.toContinueItem
import dev.aaa1115910.bv.ui.feature.timeline.DeleteConfirmDialog
import dev.aaa1115910.bv.ui.feature.timeline.DeleteModeHint
import dev.aaa1115910.bv.ui.feature.timeline.SubGridKind
import dev.aaa1115910.bv.ui.feature.timeline.SubPageFrame
import dev.aaa1115910.bv.ui.feature.timeline.SubPageGrid
import dev.aaa1115910.bv.ui.feature.timeline.SubPageLoginPrompt
import dev.aaa1115910.bv.ui.feature.timeline.deleteModeKeys
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository

/**
 * 历史记录（第 9.11 节模板）：HistoryViewModel（加载逻辑同旧电视 HistoryScreen：进入时 update()，
 * 快到底部时再 update()），视频网格 + 进度条（进度从「看到 / 总时长」算出，角标只显示总时长）。
 * 删除模式：触屏长按卡片进入，电视菜单键 / 长按确认键进入；删除模式内长按清空（二次确认），返回键退出。
 */
@Composable
fun HistoryScreen(nav: NavController) {
    val vm: HistoryViewModel = koinViewModel()
    val userRepository: BvUserRepository = koinInject()
    val login = userRepository.isLogin
    val firstRequester = remember { FocusRequester() }
    val items by remember(vm) { derivedStateOf { vm.histories.map { it.toContinueItem() } } }
    val dpad = LocalDeviceProfile.current.input == BvInputMode.DPad

    var deleteMode by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<VideoCardData?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = deleteMode) { deleteMode = false }

    LaunchedEffect(deleteMode, items.size, vm.deleting) {
        if (deleteMode && !vm.deleting && items.isEmpty()) deleteMode = false
    }

    LaunchedEffect(vm, login) {
        if (login && vm.histories.isEmpty() && !vm.updating && !vm.noMore) vm.update()
    }

    SubPageFrame(title = stringResource(R.string.history_title), onBack = { nav.popBackStack() }) { spec ->
        if (!login) {
            SubPageLoginPrompt(onLogin = { nav.navigate(LoginRoute) }, tv = spec.tv)
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .deleteModeKeys(
                        deleteMode = deleteMode,
                        onEnter = { deleteMode = true },
                        onExit = { deleteMode = false },
                        onClear = { showClearConfirm = true },
                    )
            ) {
                DeleteModeHint(
                    deleteMode = deleteMode,
                    clearSupported = true,
                    dpad = dpad,
                    modifier = Modifier.padding(start = spec.horizontal, end = spec.horizontal),
                )
                SubPageGrid(
                    list = items,
                    grid = spec.grid(SubGridKind.Video),
                    loading = vm.updating,
                    noMore = vm.noMore,
                    modifier = Modifier.weight(1f),
                    onLoadMore = { vm.update() },
                    onRetry = { vm.update() },
                    emptyMessage = stringResource(R.string.history_empty),
                    firstRequester = firstRequester,
                ) { _, item, modifier ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VideoCard(
                            data = item.card,
                            onClick = {
                                if (deleteMode) deleteTarget = item.card else nav.openVideo(item.card)
                            },
                            modifier = modifier,
                            progress = item.progress,
                            onLongClick = {
                                if (deleteMode) showClearConfirm = true else deleteMode = true
                            },
                        )
                        if (deleteMode) {
                            BvButton(
                                text = stringResource(R.string.ds_delete),
                                icon = Icons.Default.Delete,
                                onClick = { deleteTarget = item.card },
                                style = BvButtonStyle.Filled,
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                height = 32.dp,
                                horizontalPadding = 12.dp,
                                modifier = Modifier.align(Alignment.End),
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            title = stringResource(R.string.history_delete_confirm_title),
            text = stringResource(R.string.history_delete_confirm_text, target.title),
            onConfirm = {
                vm.deleteHistory(target.historyBusiness, target.historyKid)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showClearConfirm) {
        DeleteConfirmDialog(
            title = stringResource(R.string.history_clear_confirm_title),
            text = stringResource(R.string.history_clear_confirm_text),
            onConfirm = {
                vm.clearHistory()
                showClearConfirm = false
                deleteMode = false
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}
