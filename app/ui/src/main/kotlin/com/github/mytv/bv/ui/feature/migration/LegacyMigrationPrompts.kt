package com.github.mytv.bv.ui.feature.migration

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.migration.LegacyDataMigrator
import com.github.mytv.bv.migration.LegacyMigrationContract
import com.github.mytv.bv.repository.UserRepository
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.settings.SettingsDialog
import com.github.mytv.bv.ui.feature.settings.SettingsDialogActions
import com.github.mytv.bv.ui.feature.settings.tryFocusEnter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 进入应用时的自动更新检查要避开迁移提示，避免两个对话框叠在一起。 */
fun shouldShowLegacyMigrationPrompt(context: Context): Boolean = when (context.packageName) {
    LegacyMigrationContract.LEGACY_APP_ID ->
        isPackageInstalled(context, LegacyMigrationContract.NEW_APP_ID)

    LegacyMigrationContract.NEW_APP_ID ->
        LegacyDataMigrator.shouldPromptMigration(context)

    else -> false
}

@Composable
fun LegacyMigrationPrompts() {
    val context = LocalContext.current
    when (context.packageName) {
        LegacyMigrationContract.LEGACY_APP_ID -> BridgeMigrationPrompt(context)
        LegacyMigrationContract.NEW_APP_ID -> NewAppMigrationPrompt(context)
    }
}

/** 桥接版：检测到 myBV 已安装就把用户引导过去，迁移由 myBV 自己完成。 */
@Composable
private fun BridgeMigrationPrompt(context: Context) {
    var visible by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    // 安装器返回后 Activity 只会 resume，不会重建，必须在 resume 时重新探测
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                visible = isPackageInstalled(context, LegacyMigrationContract.NEW_APP_ID)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!visible || dismissed) return

    val confirmRequester = remember { FocusRequester() }
    val dismissRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        confirmRequester.tryFocusEnter()
    }
    SettingsDialog(onDismiss = { dismissed = true }) {
        Text(stringResource(R.string.legacy_bridge_prompt_title), style = BvType.PanelTitle)
        Text(
            stringResource(R.string.legacy_bridge_prompt_message),
            style = BvType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsDialogActions(
            confirm = stringResource(R.string.legacy_bridge_prompt_open),
            onConfirm = {
                dismissed = true
                openNewApp(context)
            },
            dismiss = stringResource(R.string.legacy_bridge_prompt_later),
            onDismiss = { dismissed = true },
            confirmRequester = confirmRequester,
            dismissRequester = dismissRequester,
        )
    }
}

/** 新版：刚装上、没有本地数据且桥接版仍安装时，询问后再迁移。 */
@Composable
private fun NewAppMigrationPrompt(context: Context) {
    var visible by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    var migrating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    visible = withContext(Dispatchers.IO) {
                        LegacyDataMigrator.shouldPromptMigration(context)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!visible || dismissed) return

    val confirmRequester = remember { FocusRequester() }
    val dismissRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        confirmRequester.tryFocusEnter()
    }
    SettingsDialog(onDismiss = { if (!migrating) dismissed = true }) {
        Text(stringResource(R.string.legacy_new_prompt_title), style = BvType.PanelTitle)
        Text(
            stringResource(R.string.legacy_new_prompt_message),
            style = BvType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        error?.let {
            Text(it, style = BvType.Small, color = MaterialTheme.colorScheme.error)
        }
        SettingsDialogActions(
            confirm = stringResource(
                if (migrating) R.string.legacy_new_prompt_migrating
                else R.string.legacy_new_prompt_migrate
            ),
            onConfirm = {
                if (!migrating) {
                    migrating = true
                    error = null
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching { LegacyDataMigrator.migrate(context) }.getOrDefault(false)
                        }
                        migrating = false
                        if (ok) {
                            // 迁移在 Koin 初始化之后才发生，重新把登录态灌回仓库
                            runCatching { BVApp.instance?.initRepository() }
                            runCatching { BVApp.instance?.initProxy() }
                            runCatching {
                                BVApp.koinApplication.koin.get<UserRepository>()
                                    .reloadAuthFromPrefs()
                            }
                            Toast.makeText(
                                context,
                                R.string.legacy_new_prompt_success,
                                Toast.LENGTH_LONG,
                            ).show()
                            dismissed = true
                        } else {
                            error = context.getString(R.string.legacy_new_prompt_failed)
                        }
                    }
                }
            },
            dismiss = stringResource(R.string.legacy_new_prompt_later),
            onDismiss = { if (!migrating) dismissed = true },
            confirmEnabled = !migrating,
            dismissEnabled = !migrating,
            confirmRequester = confirmRequester,
            dismissRequester = dismissRequester,
        )
    }
}

private fun openNewApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(
        LegacyMigrationContract.NEW_APP_ID
    ) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean =
    runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
