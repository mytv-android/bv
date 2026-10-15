package dev.aaa1115910.bv.ui.feature.login

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.geetest.sdk.GT3ConfigBean
import com.geetest.sdk.GT3ErrorBean
import com.geetest.sdk.GT3GeetestUtils
import com.geetest.sdk.GT3Listener
import dev.aaa1115910.biliapi.entity.login.QrLoginState
import dev.aaa1115910.biliapi.repositories.SendSmsState
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.ui.designsystem.component.AvatarShape
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.toComposeShape
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.viewmodel.login.AppQrLoginViewModel
import dev.aaa1115910.bv.viewmodel.login.GeetestResult
import dev.aaa1115910.bv.viewmodel.login.SmsLoginViewModel
import dev.aaa1115910.m3qrcode.MaterialShapeQr
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.koin.compose.koinInject

private val logger = KotlinLogging.logger("BvLogin")

/** 二维码模块颜色（原型 `.m-qr i` #1b1b1f），白底容器上的文字也用它。 */
private val QrInk = Color(0xFF1B1B1F)

// ---------- 短信登录（照抄旧电视 SmsLoginContent / 旧手机 LoginScreen 的流程，含极验） ----------

/** 短信登录的界面状态与操作。网络请求在 IO 上执行（与旧界面相同），回调切回主线程。 */
@Stable
internal class SmsLoginState(
    private val vm: SmsLoginViewModel,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    var phone by mutableStateOf("")
        private set
    var code by mutableStateOf("")
        private set

    /** 「{n} 秒后重发」倒计时。 */
    var countdown by mutableIntStateOf(0)
        private set

    internal var onSuccess: () -> Unit = {}
    internal var startCaptcha: (challenge: String, gt: String) -> Unit = { _, _ -> }
    private var countdownJob: Job? = null

    val canSend: Boolean get() = phone.length == 11 && countdown == 0
    val canLogin: Boolean get() = phone.length == 11 && code.length == 6

    fun onPhoneChange(value: String) {
        phone = value.filter { it.isDigit() }.take(11)
        // 手机号变化时清除验证数据（旧界面相同）
        vm.clearCaptchaData()
    }

    fun onCodeChange(value: String) {
        code = value.filter { it.isDigit() }.take(6)
    }

    fun sendSms() {
        if (!canSend) return
        val number = phone.toLongOrNull() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                vm.sendSms(number) { challenge, gt ->
                    scope.launch(Dispatchers.Main) { startCaptcha(challenge, gt) }
                }
            }
            withContext(Dispatchers.Main) { afterSend() }
        }
    }

    /** 极验通过后重新发送短信（旧界面 onDialogResult 的逻辑）。 */
    internal fun resendAfterCaptcha() {
        val number = phone.toLongOrNull() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { vm.sendSms(number) { _, _ -> } }
            withContext(Dispatchers.Main) { afterSend() }
        }
    }

    private fun afterSend() {
        if (vm.sendSmsState != SendSmsState.Success) return
        countdownJob?.cancel()
        countdown = 60
        countdownJob = scope.launch {
            while (countdown > 0) {
                delay(1000)
                countdown -= 1
            }
        }
    }

    fun login() {
        if (vm.sendSmsState != SendSmsState.Success) {
            Toast.makeText(context, R.string.login_send_sms_first, Toast.LENGTH_SHORT).show()
            return
        }
        val smsCode = code.toIntOrNull() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                vm.loginWithSms(smsCode) {
                    scope.launch(Dispatchers.Main) { onSuccess() }
                }
            }
        }
    }

    internal fun onGeetestResult(result: String): Boolean = runCatching {
        val geetestResult = Json.decodeFromString<GeetestResult>(result)
        vm.geetestChallenge = geetestResult.geetestChallenge
        vm.geetestValidate = geetestResult.geetestValidate
        vm.sendSmsState = SendSmsState.Ready
        resendAfterCaptcha()
    }.isSuccess

    internal fun clearCaptcha() = vm.clearCaptchaData()
}

/** 创建短信登录状态并初始化极验（GT3GeetestUtils 的配置照抄旧界面）。 */
@Composable
internal fun rememberSmsLoginState(vm: SmsLoginViewModel, onSuccess: () -> Unit): SmsLoginState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(vm) { SmsLoginState(vm, scope, context) }
    val latestSuccess by rememberUpdatedState(onSuccess)
    state.onSuccess = { latestSuccess() }
    DisposableEffect(state) {
        val utils = GT3GeetestUtils(context)
        val config = GT3ConfigBean()
        config.apply {
            pattern = 1
            isCanceledOnTouchOutside = false
            lang = null
            timeout = 10000
            webviewTimeout = 10000
            corners = 24
            listener = object : GT3Listener() {
                override fun onReceiveCaptchaCode(p0: Int) {
                    logger.info { "Geetest - onReceiveCaptchaCode: $p0" }
                }

                override fun onStatistics(p0: String?) {
                    logger.info { "Geetest - onStatistics: $p0" }
                }

                override fun onClosed(p0: Int) {
                    logger.info { "Geetest - onClosed: $p0" }
                    state.clearCaptcha()
                }

                override fun onSuccess(p0: String?) {
                    logger.info { "Geetest - onSuccess: $p0" }
                }

                override fun onFailed(p0: GT3ErrorBean?) {
                    logger.info { "Geetest - onFailed: $p0" }
                    state.clearCaptcha()
                }

                override fun onButtonClick() {
                    logger.info { "Geetest - onButtonClick" }
                }

                override fun onDialogResult(result: String) {
                    logger.info { "Geetest - onDialogResult: $result" }
                    if (state.onGeetestResult(result)) utils.showSuccessDialog() else utils.showFailedDialog()
                }
            }
        }
        utils.init(config)
        state.startCaptcha = { challenge, gt ->
            utils.startCustomFlow()
            config.api1Json = JSONObject().apply {
                put("success", 1)
                put("gt", gt)
                put("challenge", challenge)
            }
            utils.getGeetest()
        }
        onDispose {
            state.startCaptcha = { _, _ -> }
            utils.destory()
        }
    }
    return state
}

// ---------- 扫码登录（AppQrLoginViewModel） ----------

/**
 * 请求二维码并轮询结果：过期自动刷新（旧手机版），成功后等账号写入完成再回调（避免离开页面时 ViewModel 被清除、
 * 写入被取消），离开时停止轮询。
 */
@Composable
internal fun QrLoginEffect(qr: AppQrLoginViewModel, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val userRepository: UserRepository = koinInject()
    val sessionBefore = remember { userRepository.sessData }
    val latestSuccess by rememberUpdatedState(onSuccess)
    LaunchedEffect(Unit) { qr.requestQRCode() }
    LaunchedEffect(qr.state) {
        when (qr.state) {
            QrLoginState.Success -> {
                withTimeoutOrNull(5_000) {
                    snapshotFlow { userRepository.sessData }.first { it != sessionBefore }
                }
                Toast.makeText(context, R.string.login_done, Toast.LENGTH_SHORT).show()
                latestSuccess()
            }

            QrLoginState.Expired -> qr.requestQRCode()
            else -> Unit
        }
    }
    DisposableEffect(Unit) {
        onDispose { qr.cancelCheckLoginResultTimer() }
    }
}

/**
 * 二维码（现有 m3qrcode 的 Material 形状二维码），放在白底圆角 24、内边距 14 的容器里（原型 `.m-qr`）。
 * 请求中显示加载；已扫码时淡化二维码并提示在手机上确认；出错时显示重试。
 */
@Composable
internal fun LoginQrCode(qr: AppQrLoginViewModel, size: Dp, modifier: Modifier = Modifier) {
    val state = qr.state
    val showQr = qr.loginUrl.isNotBlank() &&
        (state == QrLoginState.WaitingForScan || state == QrLoginState.WaitingForConfirm || state == QrLoginState.Success)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showQr) {
            MaterialShapeQr(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (state == QrLoginState.WaitingForScan) 1f else .15f),
                content = qr.loginUrl,
            )
        }
        when (state) {
            QrLoginState.Ready, QrLoginState.RequestingQRCode -> LoadingIndicator()
            QrLoginState.WaitingForConfirm, QrLoginState.Success -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = QrInk, modifier = Modifier.size(40.dp))
                Text(
                    stringResource(if (state == QrLoginState.Success) R.string.login_done else R.string.login_qr_scanned),
                    style = BvType.BodyBold,
                    color = QrInk,
                    textAlign = TextAlign.Center,
                )
            }

            QrLoginState.Expired, QrLoginState.Error, QrLoginState.Unknown -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.login_qr_failed),
                    style = BvType.BodyBold,
                    color = QrInk,
                    textAlign = TextAlign.Center,
                )
                BvButton(text = stringResource(R.string.login_qr_retry), onClick = { qr.requestQRCode() })
            }

            QrLoginState.WaitingForScan -> Unit
        }
    }
}

/** 「用哔哩哔哩 App 扫码登录」+ Meta「二维码 3 分钟后刷新 · 已扫码请在手机上确认」。 */
@Composable
internal fun QrCaption(titleSize: Int, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.login_scan_title),
            style = TextStyle(fontSize = titleSize.sp, lineHeight = (titleSize + 6).sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.login_scan_meta),
            style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------- 通用部件 ----------

/**
 * 原型 `field()`：filled 输入框，上圆角 16 下圆角 4，surfaceContainerHighest，底部 2dp primary 指示线，
 * 内边距 8×16；标签 11sp primary，输入 16sp 等宽；可选尾部内容（「获取验证码」）。
 */
@Composable
internal fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    Column(
        modifier
            .fillMaxWidth()
            .bvFocus(scale = 1.03f, corner = 16.dp)
            .clip(shape)
            .background(scheme.surfaceContainerHighest)
            .drawBehind {
                val h = 2.dp.toPx()
                drawRect(primary, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = TextStyle(fontSize = 11.sp, lineHeight = 14.sp), color = primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontFeatureSettings = "tnum",
                    color = scheme.onSurface,
                ),
                cursorBrush = SolidColor(primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            )
            trailing?.invoke()
        }
    }
}

/** 验证码框尾部的文字按钮：「获取验证码 / {n} 秒后重发」12sp SemiBold primary。 */
@Composable
internal fun SendCodeButton(sms: SmsLoginState) {
    val enabled = sms.canSend
    Text(
        if (sms.countdown > 0) stringResource(R.string.login_resend_in, sms.countdown) else stringResource(R.string.login_get_code),
        modifier = Modifier
            .bvFocus(corner = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = sms::sendSms)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 应用图标，裁剪为 Cookie9（手机登录页顶部）。 */
@Composable
internal fun LoginAppIcon(size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(context) {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    Box(
        modifier
            .size(size)
            .clip(AvatarShape.Cookie.toComposeShape())
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                "B",
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** 对话框外框：圆角 28、surfaceContainerHigh、内边距 24。 */
@Composable
internal fun LoginDialogFrame(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                Modifier
                    .widthIn(min = 280.dp, max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        }
    }
}

/** 手机：「在另一台设备上扫码」对话框。 */
@Composable
internal fun QrLoginDialog(qr: AppQrLoginViewModel, onSuccess: () -> Unit, onDismiss: () -> Unit) {
    QrLoginEffect(qr, onSuccess = {
        onDismiss()
        onSuccess()
    })
    LoginDialogFrame(onDismiss) {
        Text(stringResource(R.string.login_qr_dialog_title), style = BvType.PanelTitle)
        LoginQrCode(qr, size = 240.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
        QrCaption(titleSize = 15, modifier = Modifier.fillMaxWidth())
        BvButton(
            text = stringResource(R.string.login_close),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            style = BvButtonStyle.Outlined,
        )
    }
}

/** 手机：「导入电视上已登录的账号」说明对话框（电视端生成 bugvideo://qrtoken 二维码，用手机扫码导入）。 */
@Composable
internal fun ImportFromTvDialog(onDismiss: () -> Unit) {
    LoginDialogFrame(onDismiss) {
        Text(stringResource(R.string.login_import_info_title), style = BvType.PanelTitle)
        Text(
            stringResource(R.string.login_import_info_body),
            style = BvType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BvButton(
            text = stringResource(R.string.login_got_it),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            style = BvButtonStyle.Tonal,
        )
    }
}
