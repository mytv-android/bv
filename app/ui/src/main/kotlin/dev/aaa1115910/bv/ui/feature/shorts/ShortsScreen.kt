package dev.aaa1115910.bv.ui.feature.shorts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.theme.BvTheme
import dev.aaa1115910.bv.ui.navigation.ShortsRoute
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.ShortsAction
import dev.aaa1115910.bv.viewmodel.ShortsEvent
import dev.aaa1115910.bv.viewmodel.ShortsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 刷刷（第 9.2 节，原型 shortsPhone / shortsTablet / shortsTv；播放器池见第 10.4 节）。
 *
 * ShortsViewModel、评论 ViewModel 与播放器池在这里创建，三种布局共享。
 * 手机（最短边 < 600dp）横过来时宽度可能超过 840dp，仍按手机处理（当前视频全屏横屏播放）。
 */
@Composable
fun ShortsScreen(nav: NavController, route: ShortsRoute) {
    val vm: ShortsViewModel = koinViewModel()
    val commentVm: CommentViewModel = koinViewModel()
    val pool = rememberShortsPlayerPool(vm)
    val comments = rememberShortsComments(commentVm)
    val actions = rememberShortsActions(vm, nav)
    val profile = LocalDeviceProfile.current
    val configuration = LocalConfiguration.current
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(route.startAid) {
        vm.start(route.startAid)
        started = true
    }
    ShortsEventToasts(vm)

    val layout = if (!profile.isTv && configuration.smallestScreenWidthDp < 600) DeviceLayout.Phone else profile.layout

    if (vm.items.isEmpty()) {
        BvTheme(forceDark = true) {
            ShortsStatus(
                vm = vm,
                started = started,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        }
    } else {
        when (layout) {
            DeviceLayout.Phone -> BvTheme(forceDark = true) { ShortsPhone(vm, pool, comments, actions) }
            // 平板右侧信息栏跟随全局明暗，只有视频区域强制深色（在 ShortsTablet 里处理）
            DeviceLayout.Tablet -> ShortsTablet(vm, pool, comments, actions)
            DeviceLayout.Tv -> BvTheme(forceDark = true) { ShortsTv(vm, pool, comments, actions) }
        }
    }
    // 手机、电视的对话框跟随刷刷的深色；平板跟随全局明暗
    BvTheme(forceDark = layout != DeviceLayout.Tablet) { ShortsActionDialogs(actions) }
}

/** ShortsViewModel 的操作结果提示（点赞 / 取消点赞不提示，按钮本身有变化） */
@Composable
private fun ShortsEventToasts(vm: ShortsViewModel) {
    val context = LocalContext.current
    LaunchedEffect(vm, context) {
        vm.events.collect { event ->
            val message: String? = when (event) {
                is ShortsEvent.ActionSucceeded -> when (event.action) {
                    ShortsAction.Coin -> context.getString(R.string.shorts_toast_coin)
                    ShortsAction.Favorite -> context.getString(R.string.shorts_toast_favorite)
                    ShortsAction.Follow -> context.getString(R.string.shorts_toast_follow)
                    ShortsAction.Unfollow -> context.getString(R.string.shorts_toast_unfollow)
                    ShortsAction.AddToView -> context.getString(R.string.shorts_toast_toview_add)
                    ShortsAction.DelToView -> context.getString(R.string.shorts_toast_toview_del)
                    ShortsAction.Like, ShortsAction.Unlike -> null
                }

                is ShortsEvent.ActionFailed -> context.getString(R.string.shorts_toast_failed, event.message.orEmpty())
                is ShortsEvent.LoginRequired -> context.getString(R.string.shorts_toast_login)
                is ShortsEvent.LoadFailed -> context.getString(R.string.shorts_toast_load_failed, event.message.orEmpty())
            }
            message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
    }
}
