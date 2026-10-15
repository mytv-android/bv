package dev.aaa1115910.bv.ui.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.viewmodel.login.AppQrLoginViewModel

/**
 * 手机登录（原型 loginPhone）：Column(padding(top = statusBar + 40, 水平 24), spacedBy 18)：
 * 应用图标 64（Cookie9）、标题 28sp、说明、手机号 / 验证码输入框、「登录」、「或」、
 * 「在另一台设备上扫码」（二维码对话框）、「导入电视上已登录的账号」（说明对话框）。
 */
@Composable
internal fun LoginPhone(sms: SmsLoginState, qr: AppQrLoginViewModel, onSuccess: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val codeRequester = remember { FocusRequester() }
    var showQr by remember { mutableStateOf(false) }
    var showImportInfo by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LoginAppIcon(64.dp)
        Text(
            stringResource(R.string.login_title),
            style = TextStyle(fontSize = 28.sp, lineHeight = 33.6.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.login_subtitle),
            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LoginField(
            label = stringResource(R.string.login_phone),
            value = sms.phone,
            onValueChange = sms::onPhoneChange,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            onImeAction = { runCatching { codeRequester.requestFocus() } },
        )
        LoginField(
            label = stringResource(R.string.login_code),
            value = sms.code,
            onValueChange = sms::onCodeChange,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            onImeAction = {
                if (sms.canLogin) {
                    keyboard?.hide()
                    sms.login()
                }
            },
            focusRequester = codeRequester,
            trailing = { SendCodeButton(sms) },
        )
        BvButton(
            text = stringResource(R.string.login_submit),
            onClick = {
                keyboard?.hide()
                sms.login()
            },
            modifier = Modifier.fillMaxWidth(),
            large = true,
            enabled = sms.canLogin,
        )
        OrDivider()
        BvButton(
            text = stringResource(R.string.login_scan_on_other_device),
            onClick = { showQr = true },
            modifier = Modifier.fillMaxWidth(),
            style = BvButtonStyle.Tonal,
            icon = Icons.Rounded.QrCode,
            large = true,
        )
        BvButton(
            text = stringResource(R.string.login_import_from_tv),
            onClick = { showImportInfo = true },
            modifier = Modifier.fillMaxWidth(),
            style = BvButtonStyle.Outlined,
        )
    }

    if (showQr) {
        QrLoginDialog(qr, onSuccess = onSuccess, onDismiss = { showQr = false })
    }
    if (showImportInfo) {
        ImportFromTvDialog(onDismiss = { showImportInfo = false })
    }
}

/** Row：分隔线 +「或」12sp onSurfaceVariant + 分隔线。 */
@Composable
private fun OrDivider() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(R.string.login_or),
            style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
