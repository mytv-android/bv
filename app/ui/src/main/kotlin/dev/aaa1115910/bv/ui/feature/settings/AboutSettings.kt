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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.network.GithubApi
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.FirebaseUtil
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.pow

private val aboutLogger = KotlinLogging.logger("BvAboutSettings")

/** 项目地址（与 GithubApi 的更新源一致）。 */
private const val GITHUB_URL = "https://github.com/ldm0206/bv"

private sealed interface LatestVersion {
    data object Loading : LatestVersion
    data object Error : LatestVersion
    data class Loaded(val name: String) : LatestVersion
}

private fun Double.toGb(): String = "${DecimalFormat("###.##").format(this / 1024.0.pow(3))} GB"

/**
 * 「关于」：版本 / 检查更新（UpdateDialog）/ 项目地址 / Alpha 更新、错误日志、FPS 显示 / 设备信息 / 解码器信息 / 崩溃测试（仅调试版）。
 * 照抄旧电视 AboutSetting、InfoSetting、OtherSetting 与旧手机 AboutContent。
 */
@Composable
internal fun AboutSettings(onMediaCodec: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var showUpdate by remember { mutableStateOf(false) }
    var updateAlpha by remember { mutableStateOf(Prefs.updateAlpha) }
    var firebase by remember { mutableStateOf(Prefs.enableFirebaseCollection) }
    var showFps by remember { mutableStateOf(Prefs.showFps) }
    var latest by remember { mutableStateOf<LatestVersion>(LatestVersion.Loading) }

    // 最新版本（切换 Alpha 更新后重新获取）
    LaunchedEffect(updateAlpha) {
        latest = LatestVersion.Loading
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
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure {
                    Toast.makeText(context, GITHUB_URL, Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    SettingSwitchRow(
        title = stringResource(R.string.settings_about_alpha),
        subtitle = stringResource(R.string.settings_about_alpha_desc),
        checked = updateAlpha,
        onCheckedChange = {
            updateAlpha = it
            Prefs.updateAlpha = it
        },
    )
    SettingSwitchRow(
        title = stringResource(R.string.settings_about_firebase),
        subtitle = stringResource(R.string.settings_about_firebase_desc),
        checked = firebase,
        onCheckedChange = {
            firebase = it
            Prefs.enableFirebaseCollection = it
            FirebaseUtil.setCrashlyticsCollectionEnabled(it)
        },
    )
    SettingSwitchRow(
        title = stringResource(R.string.settings_about_fps),
        subtitle = stringResource(R.string.settings_about_fps_desc),
        checked = showFps,
        onCheckedChange = {
            showFps = it
            Prefs.showFps = it
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
