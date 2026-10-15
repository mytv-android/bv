package dev.aaa1115910.bv.ui.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile

/**
 * 首页（第 9.1 节，原型 homePhone / homeTablet / homeTv）。
 * ViewModel 在这里创建（rememberHomeState），三种布局共享。
 */
@Composable
fun HomeScreen(nav: NavController) {
    val state = rememberHomeState()
    val layout = LocalDeviceProfile.current.layout

    // 同旧电视版 HomeContent：进入首页时更新用户信息（头像）；手机/平板在登录后加载「继续观看」
    LaunchedEffect(state.user.isLogin, layout) {
        if (state.user.isLogin) state.user.updateUserInfo()
        if (layout != DeviceLayout.Tv) state.ensureHistory()
    }

    when (layout) {
        DeviceLayout.Phone -> HomePhone(nav, state)
        DeviceLayout.Tablet -> HomeTablet(nav, state)
        DeviceLayout.Tv -> HomeTv(nav, state)
    }
}
