package com.github.mytv.bv.ui.feature.dynamic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.feature.video.CommentPicturePreviewer
import com.github.mytv.bv.ui.feature.video.rememberCommentPictures
import com.github.mytv.bv.ui.feature.video.show
import com.github.mytv.bv.ui.navigation.LoginRoute
import com.github.mytv.bv.viewmodel.CommentViewModel
import com.github.mytv.bv.viewmodel.home.DynamicViewModel
import com.github.mytv.bv.viewmodel.user.FollowViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** 三端共享的动作。 */
internal class DynamicActions(
    /** 打开一条动态：视频 → 视频详情，番剧 → 番剧详情，其他 → 动态详情。 */
    val open: (DynamicItem) -> Unit,
    /** 点图片：打开图片查看器。 */
    val onPictures: (List<Picture>, Int) -> Unit,
    val onLogin: () -> Unit,
)

/**
 * 动态（第 9.7 节，原型 dynPhone / dynTablet / dynTv）。
 * 数据：DynamicViewModel（全部动态）、FollowViewModel（头像行）、CommentViewModel（平板右栏评论）。
 */
@Composable
fun DynamicScreen(nav: NavController) {
    val vm: DynamicViewModel = koinViewModel()
    val followVm: FollowViewModel = koinViewModel()
    val commentVm: CommentViewModel = koinViewModel()
    val state = remember(vm, followVm, commentVm) { DynamicPageState(vm, followVm, commentVm) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pictures = rememberCommentPictures()
    val layout = LocalDeviceProfile.current.layout

    LaunchedEffect(state.isLogin) { state.ensureLoaded() }
    LaunchedEffect(state.upFilter, state.kind, state.items.size, state.loading) { state.autoFill() }

    val actions = remember(nav, state) {
        DynamicActions(
            open = { openDynamic(nav, context, it) },
            onPictures = { list, index -> scope.launch { pictures.show(list, index) } },
            onLogin = { nav.navigate(LoginRoute) },
        )
    }

    Box(Modifier.fillMaxSize()) {
        when (layout) {
            DeviceLayout.Phone -> DynamicPhone(state, actions)
            DeviceLayout.Tablet -> DynamicTablet(state, actions)
            DeviceLayout.Tv -> DynamicTv(state, actions)
        }
        if (layout != DeviceLayout.Tv) CommentPicturePreviewer(pictures)
    }
}
