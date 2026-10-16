package dev.aaa1115910.bv.ui.feature.toview

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.component.LocalVideoCardActions
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.focus.BvKey
import dev.aaa1115910.bv.ui.designsystem.focus.onBvKey
import dev.aaa1115910.bv.ui.feature.home.toContinueItem
import dev.aaa1115910.bv.ui.feature.timeline.SubGridKind
import dev.aaa1115910.bv.ui.feature.timeline.SubGridSpec
import dev.aaa1115910.bv.ui.feature.timeline.SubPageFrame
import dev.aaa1115910.bv.ui.feature.timeline.SubPageGrid
import dev.aaa1115910.bv.ui.feature.timeline.SubPageLoginPrompt
import dev.aaa1115910.bv.ui.feature.timeline.rememberPendingLoad
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository

/**
 * 稍后再看（第 9.11 节模板）：ToViewViewModel（进入时 update()），视频网格 + 进度条。
 * 长按（电视：长按确认 / 菜单键）弹菜单：「从稍后再看移除」（ToViewRepository.delToView）、「从这里开始刷」。
 */
@Composable
fun ToViewScreen(nav: NavController) {
    val vm: ToViewViewModel = koinViewModel()
    val userRepository: BvUserRepository = koinInject()
    val login = userRepository.isLogin
    val firstRequester = remember { FocusRequester() }
    SubPageFrame(title = stringResource(R.string.toview_title), onBack = { nav.popBackStack() }) { spec ->
        if (!login) {
            SubPageLoginPrompt(onLogin = { nav.navigate(LoginRoute) }, tv = spec.tv)
        } else {
            ToViewContent(
                vm = vm,
                grid = spec.grid(SubGridKind.Video),
                onOpen = { nav.openVideo(it) },
                modifier = Modifier.weight(1f),
                firstRequester = firstRequester,
            )
        }
    }
}

/** 稍后再看网格（「我的」平板右栏也用）。列表为空且还没加载完时自动 update()。 */
@Composable
internal fun ToViewContent(
    vm: ToViewViewModel,
    grid: SubGridSpec,
    onOpen: (VideoCardData) -> Unit,
    modifier: Modifier = Modifier,
    firstRequester: FocusRequester? = null,
) {
    val repo: ToViewRepository = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = LocalVideoCardActions.current
    val pending = rememberPendingLoad()
    val items by remember(vm) { derivedStateOf { vm.histories.map { it.toContinueItem() } } }
    var menuFor by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(vm) {
        if (vm.histories.isEmpty() && !vm.noMore) vm.update()
    }

    val remove: (Long) -> Unit = { aid ->
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { repo.delToView(aid) } }
            if (result.isSuccess) {
                vm.histories.removeAll { it.avid == aid }
                Toast.makeText(context, R.string.toview_removed, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.toview_remove_failed, result.exceptionOrNull()?.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    SubPageGrid(
        list = items,
        grid = grid,
        // ToViewViewModel 的加载中状态是私有的：列表为空且没加载完时最多按加载中显示一段时间
        loading = items.isEmpty() && !vm.noMore && pending.waiting,
        noMore = vm.noMore,
        modifier = modifier,
        onRetry = {
            pending.retry()
            vm.update()
        },
        emptyMessage = stringResource(R.string.toview_empty),
        firstRequester = firstRequester,
    ) { _, item, itemModifier ->
        val aid = item.card.avid
        Box {
            VideoCard(
                data = item.card,
                onClick = { onOpen(item.card) },
                modifier = itemModifier.onBvKey { key ->
                    if (key == BvKey.Menu) {
                        menuFor = aid
                        true
                    } else false
                },
                progress = item.progress,
                onLongClick = { menuFor = aid },
            )
            DropdownMenu(expanded = menuFor == aid, onDismissRequest = { menuFor = null }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.toview_remove)) },
                    onClick = {
                        menuFor = null
                        remove(aid)
                    },
                )
                if (actions != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ds_card_menu_shorts)) },
                        onClick = {
                            menuFor = null
                            actions.openShorts(aid)
                        },
                    )
                }
            }
        }
    }
}
