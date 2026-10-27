package com.github.mytv.bv.ui.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
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
import com.github.mytv.bv.BuildConfig
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.migration.LegacyMigrationContract
import com.github.mytv.bv.network.GithubApi
import com.github.mytv.bv.network.entity.Release
import com.github.mytv.bv.ui.PreferenceWrites
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.migration.shouldShowLegacyMigrationPrompt
import com.github.mytv.bv.util.CacheManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.content.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.UUID

private val updateLogger = KotlinLogging.logger("BvUpdateDialog")

private enum class UpdateStatus {
    UpdatingInfo, Ready, Downloading, Installing,
    NoAvailableUpdate, CheckError, DownloadError, InstallError
}

/** 用 FileProvider 打开下载好的 APK（旧 UpdateDialog.installUpdate）。 */
private fun installApk(context: Context, file: File): Boolean = runCatching {
    // 必须用实际 applicationId（debug 包带 .debug 后缀），BuildConfig.APPLICATION_ID 是 app:shared 里写死的
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}.isSuccess

private val signatureFlags: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

/** 签名证书公钥的 SHA-256；多个签名者用逗号分隔，读不到返回 null。 */
private fun PackageInfo.signerDigest(): String? = runCatching {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        signingInfo?.apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        signatures
    }
    signatures?.joinToString(",") { signature ->
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(signature.toByteArray().inputStream()) as X509Certificate
        MessageDigest.getInstance("SHA-256").digest(cert.encoded)
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}.getOrNull()

/**
 * 更新包与本机签名不一致时，系统安装器只会给一句「应用未安装」。
 * 提前判断并说明原因（本地 debug 包装 CI 包就是这种情况）；读不到签名时放行，交给系统兜底。
 */
private fun signatureMatches(context: Context, apk: File): Boolean {
    val pm = context.packageManager
    val flags = signatureFlags
    val installedInfo = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, flags)
        }
    }.getOrNull()
    val archiveInfo = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apk.absolutePath, flags)
        }
    }.getOrNull()
    val installed = installedInfo?.signerDigest()
    val archive = archiveInfo?.signerDigest()

    // 桥接版更新到 myBV 是“新装另一个应用”，允许签名换新；优先用构建时固定的证书指纹。
    if (context.packageName == LegacyMigrationContract.LEGACY_APP_ID &&
        archiveInfo?.packageName == LegacyMigrationContract.NEW_APP_ID
    ) {
        val pinned = BuildConfig.MYBV_CERT_SHA256.trim()
        if (pinned.isNotEmpty()) {
            return archive.equals(pinned, ignoreCase = true)
        }
    }

    if (installed.isNullOrEmpty() || archive.isNullOrEmpty()) return true
    return installed.equals(archive, ignoreCase = true)
}

/**
 * 检查更新对话框：逻辑照抄 app/shared 的旧 UpdateDialog（GithubApi 取最新构建 → 比较 revision →
 * 下载到 cache/update_downloader → FileProvider 安装），只换成设计系统外观。
 * 网络与文件操作在 IO 线程，状态一律回到主线程再写。
 */
@Composable
internal fun SettingsUpdateDialog(
    onDismiss: () -> Unit,
    initialRelease: Release? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(UpdateStatus.UpdatingInfo) }
    var release by remember { mutableStateOf<Release?>(null) }
    var downloaded by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(targetProgress, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "updateProgress")
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableStateOf(true) }

    val checkUpdate: () -> Unit = {
        status = UpdateStatus.UpdatingInfo
        errorDetail = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    PreferenceWrites.awaitPending()
                    val latest = GithubApi.getLatestBuild()
                    val revision = latest.revision
                        ?: throw IllegalStateException("无法从发布信息中解析版本号")
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
                    errorDetail = it.message ?: it::class.simpleName
                    status = UpdateStatus.CheckError
                }
        }
    }

    val startUpdate: () -> Unit = {
        val latest = release
        if (latest != null) {
            status = UpdateStatus.Downloading
            errorDetail = null
            downloaded = 0L
            total = 0L
            targetProgress = 0f
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val dir = File(context.cacheDir, CacheManager.UPDATE_CACHE_DIR)
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, "${UUID.randomUUID()}.apk")
                        file.createNewFile()
                        GithubApi.downloadUpdate(
                            latest,
                            file,
                            object : ProgressListener {
                                private var lastUiUpdate = 0L

                                override suspend fun onProgress(bytesSentTotal: Long, contentLength: Long?) {
                                    // 网络分块可能非常频繁：UI 最多 10Hz 更新一次
                                    val now = SystemClock.elapsedRealtime()
                                    val finished = contentLength != null && bytesSentTotal >= contentLength
                                    if (!finished && now - lastUiUpdate < 100L) return
                                    lastUiUpdate = now
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
                            // 缓存写入后检查阈值；preserve 保护刚下载的安装包不被清理
                            withContext(Dispatchers.IO) {
                                runCatching { CacheManager(context).checkCache(preserve = file) }
                            }
                            val signatureOk = withContext(Dispatchers.IO) { signatureMatches(context, file) }
                            if (!signatureOk) {
                                errorDetail = "更新包签名与当前应用不一致，无法覆盖安装"
                                status = UpdateStatus.InstallError
                            } else {
                                status = UpdateStatus.Installing
                                if (!installApk(context, file)) {
                                    errorDetail = "没有权限打开安装程序，请允许本应用安装未知应用"
                                    status = UpdateStatus.InstallError
                                }
                            }
                        }
                    }
                    .onFailure {
                        updateLogger.warn(it) { "Failed to download update" }
                        errorDetail = it.message ?: it::class.simpleName
                        status = UpdateStatus.DownloadError
                    }
            }
        }
    }

    LaunchedEffect(Unit) {
        val initial = initialRelease
        if (initial != null) {
            release = initial
            status = UpdateStatus.Ready
        } else {
            checkUpdate()
        }
    }

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

            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
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
                errorDetail?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = BvType.Small, color = MaterialTheme.colorScheme.error)
                }
            }
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

/**
 * 进入应用时静默检查一次更新，只有存在更高版本才弹窗；迁移提示出现时让路。
 * 桥接版也从这里获得“进入应用即检查更新”的行为。
 */
@Composable
internal fun LaunchUpdatePrompt() {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Release?>(null) }
    LaunchedEffect(Unit) {
        val skip = withContext(Dispatchers.IO) {
            runCatching { shouldShowLegacyMigrationPrompt(context) }.getOrDefault(false)
        }
        if (skip) return@LaunchedEffect
        val latest = withContext(Dispatchers.IO) {
            runCatching {
                PreferenceWrites.awaitPending()
                GithubApi.getLatestBuild()
            }.getOrNull()
        } ?: return@LaunchedEffect
        val revision = latest.revision ?: return@LaunchedEffect
        if (revision > BuildConfig.VERSION_CODE) pending = latest
    }
    pending?.let { latest ->
        SettingsUpdateDialog(
            onDismiss = { pending = null },
            initialRelease = latest,
        )
    }
}
