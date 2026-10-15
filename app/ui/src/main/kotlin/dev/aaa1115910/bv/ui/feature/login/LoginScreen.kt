package dev.aaa1115910.bv.ui.feature.login

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.navigation.AccountsRoute
import dev.aaa1115910.bv.viewmodel.login.AppQrLoginViewModel
import dev.aaa1115910.bv.viewmodel.login.SmsLoginViewModel
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
