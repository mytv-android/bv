package com.github.mytv.bv.ui.feature.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.AuthData
import com.github.mytv.bv.entity.BvScheme
import com.github.mytv.bv.repository.UserRepository
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.theme.BvType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private val importLogger = KotlinLogging.logger("BvQrTokenImport")

/** 解析出的 bugvideo://qrtoken 内容。 */
private class QrTokenData(
    val uid: Long,
    val username: String,
    val avatar: String,
    val auth: AuthData,
)

private fun parseQrToken(uri: Uri): Result<QrTokenData> = runCatching {
    val token = BvScheme.QrToken.fromUri(uri) as? BvScheme.QrToken
        ?: throw IllegalArgumentException("Invalid QR token URI: $uri")
    QrTokenData(
        uid = token.uid,
        username = token.username,
        avatar = token.avatar,
        auth = AuthData.fromJson(token.auth),
    )
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 导入成功后重启应用，让所有页面用新账号重新加载（与旧 QrTokenResultScreen 相同）。 */
private fun restartApp(context: Context) {
    val activity = context.findActivity()
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: activity?.let { Intent(it, it::class.java) }
    intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    activity?.finish()
    if (intent != null) context.applicationContext.startActivity(intent)
}

/**
 * bugvideo://qrtoken 导入账号对话框（移植旧手机 QrTokenResultScreen）：
 * 显示头像、用户名、UID，「添加用户」把凭证写入账号库并切换到该账号，然后重启应用。
 */
@Composable
internal fun QrTokenImportDialog(uri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository: UserRepository = koinInject()
    val parsed = remember(uri) {
        parseQrToken(uri).onFailure { importLogger.warn(it) { "Failed to parse QR token result" } }
    }
    val data = parsed.getOrNull()
    var adding by remember { mutableStateOf(false) }
    val confirmRequester = remember { FocusRequester() }
    LaunchedEffect(data) { if (data != null) runCatching { confirmRequester.requestFocus() } }

    val onConfirm: () -> Unit = {
        if (!adding && data != null) {
            adding = true
            scope.launch(Dispatchers.IO) {
                runCatching {
                    userRepository.addUser(data.auth)
                }.onFailure {
                    importLogger.error(it) { "Failed to save auth data" }
                    withContext(Dispatchers.Main) {
                        adding = false
                        Toast.makeText(context, it.message ?: it.javaClass.simpleName, Toast.LENGTH_SHORT).show()
                    }
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.login_import_added, Toast.LENGTH_SHORT).show()
                        onDismiss()
                        restartApp(context)
                    }
                }
            }
        }
    }

    LoginDialogFrame(onDismiss = { if (!adding) onDismiss() }) {
        Text(stringResource(R.string.login_import_title), style = BvType.PanelTitle)
        if (data == null) {
            Text(
                stringResource(R.string.login_import_invalid),
                style = BvType.BodyBold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                parsed.exceptionOrNull()?.message.orEmpty(),
                style = BvType.Small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BvButton(
                text = stringResource(R.string.login_close),
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
                style = BvButtonStyle.Outlined,
            )
        } else {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BvAvatar(url = data.avatar, size = 72.dp, fallbackText = data.username)
                Text(data.username, style = BvType.Title, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.login_import_uid, data.uid),
                    style = BvType.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (adding) {
                LoadingIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                Row(
                    Modifier
                        .align(Alignment.End)
                        .focusRestorer()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BvButton(
                        text = stringResource(R.string.login_import_cancel),
                        onClick = onDismiss,
                        style = BvButtonStyle.Outlined,
                    )
                    BvButton(
                        text = stringResource(R.string.login_import_add),
                        onClick = onConfirm,
                        modifier = Modifier.focusRequester(confirmRequester),
                    )
                }
            }
        }
    }
}
