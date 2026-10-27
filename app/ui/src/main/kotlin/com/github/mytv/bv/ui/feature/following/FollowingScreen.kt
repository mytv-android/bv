package com.github.mytv.bv.ui.feature.following

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.user.FollowedUser
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.BvListItem
import com.github.mytv.bv.ui.feature.timeline.SubGridKind
import com.github.mytv.bv.ui.feature.timeline.SubGridSpec
import com.github.mytv.bv.ui.feature.timeline.SubPageFrame
import com.github.mytv.bv.ui.feature.timeline.SubPageGrid
import com.github.mytv.bv.ui.feature.timeline.SubPageLoginPrompt
import com.github.mytv.bv.ui.navigation.LoginRoute
import com.github.mytv.bv.ui.navigation.SpaceRoute
import com.github.mytv.bv.viewmodel.user.FollowViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.github.mytv.bv.repository.UserRepository as BvUserRepository

/**
 * 关注的 UP 主（第 9.11 节模板）：FollowViewModel（创建时一次加载全部关注，按名字排序），
 * 列表（手机 1 列、平板 Adaptive(340)、电视 3 列），点击 → SpaceRoute(mid)。
 */
@Composable
fun FollowingScreen(nav: NavController) {
    val vm: FollowViewModel = koinViewModel()
    val userRepository: BvUserRepository = koinInject()
    val firstRequester = remember { FocusRequester() }
    SubPageFrame(title = stringResource(R.string.following_title), onBack = { nav.popBackStack() }) { spec ->
        if (!userRepository.isLogin) {
            SubPageLoginPrompt(onLogin = { nav.navigate(LoginRoute) }, tv = spec.tv)
        } else {
            FollowingContent(
                vm = vm,
                grid = spec.grid(SubGridKind.List),
                onOpen = { nav.navigate(SpaceRoute(it.mid)) },
                modifier = Modifier.weight(1f),
                firstRequester = firstRequester,
            )
        }
    }
}

/** UP 主列表（「我的」平板右栏用 2 列）。 */
@Composable
internal fun FollowingContent(
    vm: FollowViewModel,
    grid: SubGridSpec,
    onOpen: (FollowedUser) -> Unit,
    modifier: Modifier = Modifier,
    firstRequester: FocusRequester? = null,
) {
    SubPageGrid(
        list = vm.followedUsers,
        grid = grid,
        loading = vm.updating,
        noMore = true,
        modifier = modifier,
        emptyMessage = stringResource(R.string.following_empty),
        firstRequester = firstRequester,
        itemKey = { it.mid },
    ) { _, up, itemModifier ->
        BvListItem(
            title = up.name,
            modifier = itemModifier,
            subtitle = up.sign.trim().ifEmpty { null },
            onClick = { onOpen(up) },
            leading = { BvAvatar(url = up.avatar.ifBlank { null }, size = 40.dp, fallbackText = up.name) },
        )
    }
}
