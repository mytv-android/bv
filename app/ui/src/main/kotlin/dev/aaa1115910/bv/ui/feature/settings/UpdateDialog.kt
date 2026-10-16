package dev.aaa1115910.bv.ui.feature.settings

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.network.GithubApi
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.content.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val updateLogger = KotlinLogging.logger("BvUpdateDialog")

private enum class UpdateStatus {
    UpdatingInfo, Ready, Downloading, Installing,
    NoAvailableUpdate, CheckError, DownloadError, InstallError
}

/** 用 FileProvider 打开下载好的 APK（旧 UpdateDialog.installUpdate）。 */
private fun installApk(context: Context, file: File): Boolean = runCatching {
    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}.isSuccess

/**
 * 检查更新对话框：逻辑照抄 app/shared 的旧 UpdateDialog（GithubApi 取最新构建 → 比较 revision →
 * 下载到 cache/update_downloader → FileProvider 安装），只换成设计系统外观。
 * 网络与文件操作在 IO 线程，状态一律回到主线程再写。
 */
@Composable
internal fun SettingsUpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(UpdateStatus.UpdatingInfo) }
    var release by remember { mutableStateOf<Release?>(null) }
    var downloaded by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(targetProgress, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "updateProgress")
    var active by remember { mutableStateOf(true) }

    val checkUpdate: () -> Unit = {
        status = UpdateStatus.UpdatingInfo
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val latest = GithubApi.getLatestBuild()
                    val revision = latest.assets.first { it.name.startsWith("BV") }.name.split("_")[1].toInt()
                    latest to (revision > BuildConfig.VERSION_CODE)
                }
            }
            result
                .onSuccess { (latest, hasUpdate) ->
                    release = latest
                    updateLogger.info { "Find latest version ${latest.name}" }
                    status = if (hasUpdate) UpdateStatus.Ready else UpdateStatus.NoAvailableUpdate
                }
                .onFailure {
                    updateLogger.warn(it) { "Failed to get latest version" }
                    status = UpdateStatus.CheckError
                }
        }
    }

    val startUpdate: () -> Unit = {
        val latest = release
        if (latest != null) {
            status = UpdateStatus.Downloading
            downloaded = 0L
            total = 0L
            targetProgress = 0f
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(context.cacheDir, "update_downloader")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, "${UUID.randomUUID()}.apk")
                        file.createNewFile()
                        GithubApi.downloadUpdate(
                            latest,
                            file,
                            object : ProgressListener {
                                override suspend fun onProgress(bytesSentTotal: Long, contentLength: Long?) {
                                    withContext(Dispatchers.Main) {
                                        downloaded = bytesSentTotal
                                        total = contentLength ?: 0L
                                        targetProgress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                                    }
                                }
                            },
                        )
                        file
                    }
                }
                result
                    .onSuccess { file ->
                        if (active) {
                            status = UpdateStatus.Installing
                            if (!installApk(context, file)) status = UpdateStatus.InstallError
                        }
                    }
                    .onFailure {
                        updateLogger.warn(it) { "Failed to download update" }
                        status = UpdateStatus.DownloadError
                    }
            }
        }
    }

    LaunchedEffect(Unit) { checkUpdate() }

    val busy = status == UpdateStatus.Downloading || status == UpdateStatus.Installing
    val dismiss: () -> Unit = {
        if (!busy) {
            active = false
            onDismiss()
        }
    }
    val dismissRequester = remember { FocusRequester() }
    val confirmRequester = remember { FocusRequester() }
    val hasConfirm = status == UpdateStatus.Ready || status == UpdateStatus.CheckError ||
        status == UpdateStatus.DownloadError || status == UpdateStatus.InstallError
    LaunchedEffect(status) {
        withFrameNanos { }
        if (hasConfirm) confirmRequester.tryFocusEnter() else dismissRequester.tryFocusEnter()
    }

    SettingsDialog(onDismiss = dismiss) {
        Text(
            when (status) {
                UpdateStatus.UpdatingInfo -> stringResource(R.string.settings_update_title_checking)
                UpdateStatus.Ready -> release?.name.orEmpty()
                UpdateStatus.Downloading -> stringResource(R.string.settings_update_title_downloading)
                UpdateStatus.Installing -> stringResource(R.string.settings_update_title_installing)
                UpdateStatus.NoAvailableUpdate -> stringResource(R.string.settings_update_title_none)
                UpdateStatus.CheckError -> stringResource(R.string.settings_update_title_check_error)
                UpdateStatus.DownloadError -> stringResource(R.string.settings_update_title_download_error)
                UpdateStatus.InstallError -> stringResource(R.string.settings_update_title_install_error)
            },
            style = BvType.PanelTitle,
        )
        when (status) {
            UpdateStatus.UpdatingInfo -> LoadingIndicator()
            UpdateStatus.Ready -> Text(
                release?.body?.takeIf { it.isNotBlank() } ?: stringResource(R.string.settings_update_body_empty),
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                style = BvType.Small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            UpdateStatus.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.settings_update_progress, downloaded, total),
                    modifier = Modifier.fillMaxWidth(),
                    style = BvType.Label.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }

            else -> Text(
                stringResource(
                    when (status) {
                        UpdateStatus.Installing -> R.string.settings_update_body_installing
                        UpdateStatus.DownloadError -> R.string.settings_update_body_download_error
                        UpdateStatus.InstallError -> R.string.settings_update_body_install_error
                        UpdateStatus.CheckError -> R.string.settings_update_body_check_error
                        else -> R.string.settings_update_body_none
                    }
                ),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsDialogActions(
            confirm = when (status) {
                UpdateStatus.Ready -> stringResource(R.string.settings_update_now)
                UpdateStatus.CheckError, UpdateStatus.DownloadError, UpdateStatus.InstallError ->
                    stringResource(R.string.settings_update_retry)

                else -> null
            },
            onConfirm = {
                if (status == UpdateStatus.Ready) startUpdate() else checkUpdate()
            },
            dismiss = stringResource(
                when (status) {
                    UpdateStatus.UpdatingInfo -> R.string.settings_update_dismiss_checking
                    UpdateStatus.Ready -> R.string.settings_update_dismiss_ready
                    UpdateStatus.NoAvailableUpdate -> R.string.settings_update_dismiss_none
                    UpdateStatus.Downloading, UpdateStatus.Installing -> R.string.settings_update_dismiss_busy
                    else -> R.string.settings_update_dismiss_error
                }
            ),
            onDismiss = dismiss,
            dismissEnabled = !busy,
            confirmRequester = confirmRequester,
            dismissRequester = dismissRequester,
        )
    }
}
