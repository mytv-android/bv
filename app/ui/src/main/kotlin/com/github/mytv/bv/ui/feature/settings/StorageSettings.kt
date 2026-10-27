package com.github.mytv.bv.ui.feature.settings

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
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.util.CacheManager
import com.github.mytv.bv.util.LogCatcherUtil
import com.github.mytv.bv.util.Prefs
import com.github.mytv.bv.ui.PreferenceWrites
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val storageLogger = KotlinLogging.logger("BvStorageSettings")

/** 缓存上限可选项（MB），0 表示不限制。 */
private val CacheThresholdOptions =
    listOf(50, 100, 200, 500, CacheManager.THRESHOLD_UNLIMITED)

/** 可清理的三类文件（旧电视 StorageSetting）。 */
private enum class StorageTarget(@param:StringRes val title: Int) {
    ImageCache(R.string.settings_store_image),
    OthersCache(R.string.settings_store_others),
    CrashLogs(R.string.settings_store_crash);

    fun dir(context: Context): File = when (this) {
        ImageCache -> File(context.cacheDir, CacheManager.IMAGE_CACHE_DIR)
        OthersCache -> File(context.cacheDir, CacheManager.UPDATE_CACHE_DIR)
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
internal fun StorageSettings(onLogs: () -> Unit, container: SettingsContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sizes by remember { mutableStateOf<Map<StorageTarget, Long>?>(null) }
    var pending by remember { mutableStateOf<StorageTarget?>(null) }
    val initial = rememberPreferenceSnapshot {
        StoragePreferences(
            threshold = Prefs.cacheThreshold,
            autoClean = Prefs.cacheAutoClean,
        )
    } ?: return
    var threshold by remember { mutableStateOf(initial.threshold) }
    var autoClean by remember { mutableStateOf(initial.autoClean) }

    val refresh: () -> Unit = {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                StorageTarget.entries.associateWith { runCatching { folderSize(it.dir(context)) }.getOrDefault(0L) }
            }
            sizes = result
        }
    }
    LaunchedEffect(Unit) { refresh() }

    // 阈值调低 / 重新开启自动清理时立即生效，并刷新各目录大小显示
    val cleanupNow: () -> Unit = {
        scope.launch {
            PreferenceWrites.awaitPending()
            withContext(Dispatchers.IO) {
                runCatching { CacheManager(context).checkCache() }
            }
            refresh()
        }
    }

    container {
        SettingChoiceGroup(
            label = stringResource(R.string.settings_store_threshold),
            options = CacheThresholdOptions,
            isSelected = { it == threshold },
            optionLabel = {
                if (it == CacheManager.THRESHOLD_UNLIMITED) {
                    stringResource(R.string.settings_store_threshold_unlimited)
                } else {
                    stringResource(R.string.settings_store_size, it)
                }
            },
            onSelect = {
                threshold = it
                PreferenceWrites.enqueue { Prefs.cacheThreshold = it }
                cleanupNow()
            },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_store_auto_clean),
            subtitle = stringResource(R.string.settings_store_auto_clean_desc),
            checked = autoClean,
            onCheckedChange = {
                autoClean = it
                PreferenceWrites.enqueue { Prefs.cacheAutoClean = it }
                if (it) cleanupNow()
            },
        )

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
}

private data class StoragePreferences(
    val threshold: Int,
    val autoClean: Boolean,
)
