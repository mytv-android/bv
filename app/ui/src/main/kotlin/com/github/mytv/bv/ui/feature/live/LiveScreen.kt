package com.github.mytv.bv.ui.feature.live

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.github.mytv.bv.repository.UserRepository
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.viewmodel.live.LiveHomeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * 直播（首页第 9.x 节，对齐 home 的三端组织）。
 * LiveHomeViewModel 在这里创建（koinViewModel，挂当前导航项），三种布局共享。
 */
@Composable
fun LiveScreen(nav: NavController) {
    val vm: LiveHomeViewModel = koinViewModel()
    val userRepository: UserRepository = koinInject()
    val login = userRepository.isLogin

    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> LivePhone(nav, vm, login)
        DeviceLayout.Tablet -> LiveTablet(nav, vm, login)
        DeviceLayout.Tv -> LiveTv(nav, vm, login)
    }
}
