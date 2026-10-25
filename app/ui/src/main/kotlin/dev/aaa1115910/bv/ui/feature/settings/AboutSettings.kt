package dev.aaa1115910.bv.ui.feature.settings

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.network.GithubApi
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.FirebaseUtil
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.ui.PreferenceWrites
import dev.aaa1115910.m3qrcode.MaterialShapeQr
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.pow

private val aboutLogger = KotlinLogging.logger("BvAboutSettings")

/** 项目地址（与 GithubApi 的更新源一致）。 */
private const val GITHUB_URL = "https://github.com/mytv-android/bv"

/** Telegram 交流群。 */
private const val TELEGRAM_URL = "https://t.me/mytv_android_channel"

/** 打开外部链接；没有能处理的 App 时退回 Toast 展示地址。 */
private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        Toast.makeText(context, url, Toast.LENGTH_LONG).show()
    }
}

private sealed interface LatestVersion {
    data object Loading : LatestVersion
    data object Error : LatestVersion
    data class Loaded(val name: String) : LatestVersion
}

/** 二维码对话框要展示的链接：标题用字符串资源，地址直接拿来编码。 */
private data class LinkEntry(@StringRes val title: Int, val url: String)

private fun Double.toGb(): String = "${DecimalFormat("###.##").format(this / 1024.0.pow(3))} GB"

/**
 * 「关于」：版本 / 检查更新（UpdateDialog）/ 项目地址 / Telegram 群 / Alpha 更新、错误日志、FPS 显示 / 设备信息 / 解码器信息 / 崩溃测试（仅调试版）。
 * 照抄旧电视 AboutSetting、InfoSetting、OtherSetting 与旧手机 AboutContent。
 */
@Composable
internal fun AboutSettings(onMediaCodec: () -> Unit, container: SettingsContainer) {
    val context = LocalContext.current
    val view = LocalView.current
    val isTv = LocalDeviceProfile.current.isTv
    var showUpdate by remember { mutableStateOf(false) }
    val initial = rememberPreferenceSnapshot {
        AboutPreferences(
            updateAlpha = Prefs.updateAlpha,
            firebase = Prefs.enableFirebaseCollection,
            showFps = Prefs.showFps,
        )
    } ?: return
    var updateAlpha by remember { mutableStateOf(initial.updateAlpha) }
    var firebase by remember { mutableStateOf(initial.firebase) }
    var showFps by remember { mutableStateOf(initial.showFps) }
    var latest by remember { mutableStateOf<LatestVersion>(LatestVersion.Loading) }
    var linkQr by remember { mutableStateOf<LinkEntry?>(null) }

    // 最新版本（切换 Alpha 更新后重新获取）
    LaunchedEffect(updateAlpha) {
        latest = LatestVersion.Loading
        PreferenceWrites.awaitPending()
        latest = withContext(Dispatchers.IO) {
            runCatching { GithubApi.getLatestBuild().name }
                .onFailure { aboutLogger.warn(it) { "Failed to get latest version" } }
                .fold(onSuccess = { LatestVersion.Loaded(it) }, onFailure = { LatestVersion.Error })
        }
    }

    val latestText = when (val v = latest) {
        LatestVersion.Loading -> stringResource(R.string.settings_about_latest_loading)
        LatestVersion.Error -> stringResource(R.string.settings_about_latest_error)
        is LatestVersion.Loaded -> stringResource(R.string.settings_about_latest, v.name)
    }

    // 电视上通常没有浏览器，点链接行改为弹二维码让手机扫码；手机 / 平板照旧直接打开网页
    val onLinkClick: (LinkEntry) -> Unit = { entry ->
        if (isTv) linkQr = entry else openUrl(context, entry.url)
    }

    container {
        SettingRows(stringResource(R.string.settings_about_group_version)) {
            SettingNavRow(
                title = stringResource(R.string.settings_about_version),
                subtitle = "${BuildConfig.VERSION_NAME}.${BuildConfig.BUILD_TYPE}",
                value = latestText,
                onClick = { showUpdate = true },
            )
            SettingNavRow(
                title = stringResource(R.string.settings_about_github),
                subtitle = GITHUB_URL,
                onClick = { onLinkClick(LinkEntry(R.string.settings_about_github, GITHUB_URL)) },
            )
            SettingNavRow(
                title = stringResource(R.string.settings_about_telegram),
                subtitle = TELEGRAM_URL,
                onClick = { onLinkClick(LinkEntry(R.string.settings_about_telegram, TELEGRAM_URL)) },
            )
        }

        SettingSwitchRow(
            title = stringResource(R.string.settings_about_alpha),
            subtitle = stringResource(R.string.settings_about_alpha_desc),
            checked = updateAlpha,
            onCheckedChange = {
                updateAlpha = it
                PreferenceWrites.enqueue { Prefs.updateAlpha = it }
            },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_about_firebase),
            subtitle = stringResource(R.string.settings_about_firebase_desc),
            checked = firebase,
            onCheckedChange = {
                firebase = it
                PreferenceWrites.enqueue { Prefs.enableFirebaseCollection = it }
                FirebaseUtil.setCrashlyticsCollectionEnabled(it)
            },
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_about_fps),
            subtitle = stringResource(R.string.settings_about_fps_desc),
            checked = showFps,
            onCheckedChange = {
                showFps = it
                PreferenceWrites.enqueue { Prefs.showFps = it }
            },
        )

        // 设备信息（旧电视 InfoSetting）
        val unknown = stringResource(R.string.settings_device_unknown)
        val memory = remember { memoryInfo(context) }
        val storage = remember { storageInfo() }
        val screen = remember(view) { screenInfo(view, context) }
        SettingGroup(stringResource(R.string.settings_about_group_device)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DeviceInfoLine(stringResource(R.string.settings_device_manufacturer, Build.MANUFACTURER))
                DeviceInfoLine(stringResource(R.string.settings_device_model, Build.MODEL, Build.PRODUCT))
                DeviceInfoLine(stringResource(R.string.settings_device_system, Build.VERSION.RELEASE))
                if (screen != null) {
                    DeviceInfoLine(stringResource(R.string.settings_device_screen, screen.first, screen.second, screen.third))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    DeviceInfoLine(stringResource(R.string.settings_device_soc, Build.SOC_MANUFACTURER, Build.SOC_MODEL))
                }
                DeviceInfoLine(
                    stringResource(
                        R.string.settings_device_memory,
                        memory?.first ?: unknown,
                        memory?.second ?: unknown,
                    )
                )
                DeviceInfoLine(
                    stringResource(
                        R.string.settings_device_storage,
                        storage?.first ?: unknown,
                        storage?.second ?: unknown,
                    )
                )
            }
        }

        SettingRows(stringResource(R.string.settings_about_group_debug)) {
            SettingNavRow(
                title = stringResource(R.string.settings_about_codec),
                subtitle = stringResource(R.string.settings_about_codec_desc),
                onClick = onMediaCodec,
            )
            if (BuildConfig.DEBUG) {
                SettingNavRow(
                    title = stringResource(R.string.settings_about_crash),
                    subtitle = stringResource(R.string.settings_about_crash_desc),
                    onClick = { throw Exception("Boom!") },
                    showChevron = false,
                )
            }
        }

        if (showUpdate) {
            SettingsUpdateDialog(onDismiss = { showUpdate = false })
        }

        linkQr?.let { entry ->
            LinkQrCodeDialog(
                title = stringResource(entry.title),
                link = entry.url,
                onDismiss = { linkQr = null },
            )
        }
    }
}

/**
 * 链接二维码对话框：全屏遮罩 + 大二维码，方便电视用户拿手机扫。
 * 二维码用 app/shared 的 MaterialShapeQr（与登录页、日志导出同一套），下方展示原始链接，
 * 有浏览器的设备仍可以点「打开链接」直接访问。
 */
@Composable
private fun LinkQrCodeDialog(title: String, link: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val openRequester = remember { FocusRequester() }
    val dismissRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        openRequester.tryFocusEnter()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            val qrSize = minOf(maxWidth * 0.5f, maxHeight * 0.5f, 300.dp)
            Surface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(0.85f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = title,
                        style = BvType.PanelTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .size(qrSize)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MaterialShapeQr(modifier = Modifier.fillMaxSize(), content = link)
                    }
                    Text(
                        text = link,
                        style = BvType.Small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.settings_about_qr_hint),
                        style = BvType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SettingsDialogActions(
                        confirm = stringResource(R.string.settings_about_qr_open),
                        onConfirm = { openUrl(context, link) },
                        dismiss = stringResource(R.string.settings_about_qr_close),
                        onDismiss = onDismiss,
                        confirmRequester = openRequester,
                        dismissRequester = dismissRequester,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoLine(text: String) {
    Text(text, style = BvType.Small, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** 可用内存 / 总内存。 */
private fun memoryInfo(context: Context): Pair<String, String>? = runCatching {
    val info = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
    info.availMem.toDouble().toGb() to info.totalMem.toDouble().toGb()
}.getOrNull()

/** 可用存储 / 总存储。 */
@Suppress("DEPRECATION")
private fun storageInfo(): Pair<String, String>? = runCatching {
    val statFs = StatFs(Environment.getExternalStorageDirectory().absolutePath)
    statFs.availableBytes.toDouble().toGb() to statFs.totalBytes.toDouble().toGb()
}.getOrNull()

/** 屏幕物理分辨率与刷新率。 */
@Suppress("DEPRECATION")
private fun screenInfo(view: View, context: Context): Triple<Int, Int, Float>? = runCatching {
    val display = view.display
        ?: (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val mode = display.mode
    Triple(mode.physicalWidth, mode.physicalHeight, mode.refreshRate)
}.getOrNull()

private data class AboutPreferences(
    val updateAlpha: Boolean,
    val firebase: Boolean,
    val showFps: Boolean,
)
