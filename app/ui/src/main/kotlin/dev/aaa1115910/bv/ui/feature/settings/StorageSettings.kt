package dev.aaa1115910.bv.ui.feature.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.LogCatcherUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val storageLogger = KotlinLogging.logger("BvStorageSettings")

/** 可清理的三类文件（旧电视 StorageSetting）。 */
private enum class StorageTarget(@param:StringRes val title: Int) {
    ImageCache(R.string.settings_store_image),
    OthersCache(R.string.settings_store_others),
    CrashLogs(R.string.settings_store_crash);

    fun dir(context: Context): File = when (this) {
        ImageCache -> File(context.cacheDir, "image_cache")
        OthersCache -> File(context.cacheDir, "update_downloader")
        CrashLogs -> File(context.filesDir, LogCatcherUtil.LOG_DIR)
    }
}

private fun folderSize(file: File): Long =
    if (file.isDirectory) file.listFiles()?.sumOf { folderSize(it) } ?: 0L else file.length()

private fun Long.toMb(): Long = this / 1024 / 1024

/**
 * 「存储」：图片缓存、其他缓存、崩溃日志的占用与清理（确认对话框），以及日志页入口。
 * 逻辑照抄旧电视 StorageSetting；文件大小在 IO 线程计算，回到主线程再写状态。
 */
@Composable
internal fun StorageSettings(onLogs: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sizes by remember { mutableStateOf<Map<StorageTarget, Long>?>(null) }
    var pending by remember { mutableStateOf<StorageTarget?>(null) }

    val refresh: () -> Unit = {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                StorageTarget.entries.associateWith { runCatching { folderSize(it.dir(context)) }.getOrDefault(0L) }
            }
            sizes = result
        }
    }
    LaunchedEffect(Unit) { refresh() }

    val calculating = stringResource(R.string.settings_store_calculating)
    SettingRows(stringResource(R.string.settings_store_group_cache)) {
        StorageTarget.entries.forEach { target ->
            val size = sizes?.get(target)
            SettingNavRow(
                title = stringResource(target.title),
                value = if (size == null) calculating else stringResource(R.string.settings_store_size, size.toMb()),
                showChevron = false,
                onClick = { pending = target },
            )
        }
    }

    SettingRows(stringResource(R.string.settings_store_group_logs)) {
        SettingNavRow(
            title = stringResource(R.string.settings_store_logs),
            subtitle = stringResource(R.string.settings_store_logs_desc),
            onClick = onLogs,
        )
    }

    pending?.let { target ->
        val size = sizes?.get(target) ?: 0L
        val confirmRequester = remember { FocusRequester() }
        LaunchedEffect(target) {
            withFrameNanos { }
            confirmRequester.tryFocusEnter()
        }
        SettingsDialog(onDismiss = { pending = null }) {
            Text(
                stringResource(R.string.settings_store_clear_title, stringResource(target.title)),
                style = BvType.PanelTitle,
            )
            Text(
                stringResource(R.string.settings_store_clear_body, size.toMb()),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsDialogActions(
                confirm = stringResource(R.string.settings_confirm),
                onConfirm = {
                    pending = null
                    scope.launch {
                        storageLogger.info { "clear ${target.name}" }
                        withContext(Dispatchers.IO) { runCatching { target.dir(context).deleteRecursively() } }
                        refresh()
                    }
                },
                dismiss = stringResource(R.string.settings_cancel),
                onDismiss = { pending = null },
                confirmRequester = confirmRequester,
            )
        }
    }
}
