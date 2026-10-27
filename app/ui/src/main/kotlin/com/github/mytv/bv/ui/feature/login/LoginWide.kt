package com.github.mytv.bv.ui.feature.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.AvatarShape
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.toComposeShape
import com.github.mytv.bv.viewmodel.login.AppQrLoginViewModel

private fun FocusRequester.tryFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/**
 * 平板 / 电视登录（原型 loginTablet / loginTv，同一布局，括号内为电视数值）：
 * Row(居中, spacedBy 80 (56), 水平 48)：左二维码 300 (230) + 说明；右 Column(宽 400 (320), spacedBy 16 (12))：
 * 标题 32sp (26sp)、3 个步骤、分隔线、「改用手机号和验证码」（电视初始焦点，切换为短信表单）、「切换到已保存的账号」。
 */
@Composable
internal fun LoginWide(
    sms: SmsLoginState,
    qr: AppQrLoginViewModel,
    tv: Boolean,
    onSuccess: () -> Unit,
    onSwitchAccount: () -> Unit,
) {
    var smsMode by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val smsRequester = remember { FocusRequester() }
    val phoneRequester = remember { FocusRequester() }
    val codeRequester = remember { FocusRequester() }

    QrLoginEffect(qr, onSuccess)
    BackHandler(enabled = smsMode) {
        keyboard?.hide()
        smsMode = false
    }
    // 电视：初始焦点「改用手机号和验证码」；切到短信表单后焦点在手机号，切回后回到按钮
    LaunchedEffect(smsMode) {
        if (!tv) return@LaunchedEffect
        withFrameNanos { }
        if (smsMode) phoneRequester.tryFocus() else smsRequester.tryFocus()
    }

    val gap = if (tv) 56.dp else 80.dp
    val rightGap = if (tv) 12.dp else 16.dp
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .padding(horizontal = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.width(if (tv) 260.dp else 340.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LoginQrCode(qr, size = if (tv) 230.dp else 300.dp)
                QrCaption(titleSize = if (tv) 15 else 17)
            }
            Column(
                Modifier
                    .width(if (tv) 320.dp else 400.dp)
                    .focusRestorer()
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(rightGap),
            ) {
                Text(
                    stringResource(R.string.login_title),
                    style = TextStyle(
                        fontSize = if (tv) 26.sp else 32.sp,
                        lineHeight = if (tv) 31.2.sp else 38.4.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!smsMode) {
                    LoginStep(1, stringResource(R.string.login_step_1))
                    LoginStep(2, stringResource(R.string.login_step_2))
                    LoginStep(3, stringResource(R.string.login_step_3))
                } else {
                    LoginField(
                        label = stringResource(R.string.login_phone),
                        value = sms.phone,
                        onValueChange = sms::onPhoneChange,
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                        onImeAction = { runCatching { codeRequester.requestFocus() } },
                        focusRequester = phoneRequester,
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
                        enabled = sms.canLogin,
                    )
                }
                HorizontalDivider(
                    Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                if (!smsMode) {
                    BvButton(
                        text = stringResource(R.string.login_use_sms),
                        onClick = { smsMode = true },
                        modifier = Modifier.focusRequester(smsRequester),
                        style = BvButtonStyle.Tonal,
                        icon = Icons.Rounded.PhoneAndroid,
                    )
                } else {
                    BvButton(
                        text = stringResource(R.string.login_use_qr),
                        onClick = {
                            keyboard?.hide()
                            smsMode = false
                        },
                        style = BvButtonStyle.Tonal,
                        icon = Icons.Rounded.QrCode,
                    )
                }
                BvButton(
                    text = stringResource(R.string.login_switch_account),
                    onClick = onSwitchAccount,
                    style = BvButtonStyle.Outlined,
                    icon = Icons.Rounded.Person,
                )
            }
        }
    }
}

/** 步骤行：编号头像 30dp（1、3 Cookie9 primaryContainer；2 Sunny secondaryContainer，13sp）+ 14sp 文字。 */
@Composable
private fun LoginStep(number: Int, text: String) {
    val sunny = number == 2
    val scheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip((if (sunny) AvatarShape.Sunny else AvatarShape.Cookie).toComposeShape())
                .background(if (sunny) scheme.secondaryContainer else scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
                color = if (sunny) scheme.onSecondaryContainer else scheme.onPrimaryContainer,
            )
        }
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            color = scheme.onSurface,
        )
    }
}
