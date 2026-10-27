package com.github.mytv.bv.ui.feature.login

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.navigation.AccountsRoute
import com.github.mytv.bv.viewmodel.login.AppQrLoginViewModel
import com.github.mytv.bv.viewmodel.login.SmsLoginViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 登录（第 9.10 节，原型 loginPhone / loginTablet / loginTv）。
 * SmsLoginViewModel（含极验）与 AppQrLoginViewModel 在这里创建；登录成功后返回上一页。
 */
@Composable
fun LoginScreen(nav: NavController) {
    val smsVm: SmsLoginViewModel = koinViewModel()
    val qrVm: AppQrLoginViewModel = koinViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val onSuccess: () -> Unit = {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) nav.popBackStack()
    }
    val sms = rememberSmsLoginState(smsVm, onSuccess)
    when (val layout = LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> LoginPhone(sms, qrVm, onSuccess)
        DeviceLayout.Tablet, DeviceLayout.Tv -> LoginWide(
            sms = sms,
            qr = qrVm,
            tv = layout == DeviceLayout.Tv,
            onSuccess = onSuccess,
            onSwitchAccount = { nav.navigate(AccountsRoute) },
        )
    }
}
