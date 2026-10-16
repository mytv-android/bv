package dev.aaa1115910.bv.ui.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.feature.home.toContinueItem
import dev.aaa1115910.bv.ui.feature.timeline.SubGridKind
import dev.aaa1115910.bv.ui.feature.timeline.SubPageFrame
import dev.aaa1115910.bv.ui.feature.timeline.SubPageGrid
import dev.aaa1115910.bv.ui.feature.timeline.SubPageLoginPrompt
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository

/**
 * 历史记录（第 9.11 节模板）：HistoryViewModel（加载逻辑同旧电视 HistoryScreen：进入时 update()，
 * 快到底部时再 update()），视频网格 + 进度条（进度从「看到 / 总时长」算出，角标只显示总时长）。
 */
@Composable
fun HistoryScreen(nav: NavController) {
    val vm: HistoryViewModel = koinViewModel()
    val userRepository: BvUserRepository = koinInject()
    val login = userRepository.isLogin
    val firstRequester = remember { FocusRequester() }
    val items by remember(vm) { derivedStateOf { vm.histories.map { it.toContinueItem() } } }

    LaunchedEffect(vm, login) {
        if (login && vm.histories.isEmpty() && !vm.updating && !vm.noMore) vm.update()
    }

    SubPageFrame(title = stringResource(R.string.history_title), onBack = { nav.popBackStack() }) { spec ->
        if (!login) {
            SubPageLoginPrompt(onLogin = { nav.navigate(LoginRoute) }, tv = spec.tv)
        } else {
            SubPageGrid(
                list = items,
                grid = spec.grid(SubGridKind.Video),
                loading = vm.updating,
                noMore = vm.noMore,
                onLoadMore = { vm.update() },
                onRetry = { vm.update() },
                emptyMessage = stringResource(R.string.history_empty),
                firstRequester = firstRequester,
            ) { _, item, modifier ->
                VideoCard(
                    data = item.card,
                    onClick = { nav.openVideo(item.card) },
                    modifier = modifier,
                    progress = item.progress,
                )
            }
        }
    }
}
