package dev.aaa1115910.bv.ui.feature.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvListItem
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.LogCatcherUtil
import dev.aaa1115910.m3qrcode.MaterialShapeQr
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

private val logsLogger = KotlinLogging.logger("BvLogsScreen")

/** 局域网 IP（照抄旧电视 LogsScreen.getIpAddress）。 */
@Suppress("DEPRECATION")
private fun ipAddress(context: Context): String = runCatching {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    var ip = ""
    if (isWifi) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipNum = wifiManager.connectionInfo.ipAddress
        ip = "${ipNum and 0xFF}.${ipNum shr 8 and 0xFF}.${ipNum shr 16 and 0xFF}.${ipNum shr 24 and 0xFF}"
    }
    if (ip.isEmpty() || ip == "0.0.0.0") {
        ip = ""
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (ip.isEmpty() && interfaces.hasMoreElements()) {
            val addresses = interfaces.nextElement().inetAddresses
            while (ip.isEmpty() && addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is Inet4Address) ip = address.hostAddress ?: ""
            }
        }
    }
    ip
}.getOrDefault("")

private data class LogEntry(
    val file: File,
    val sizeKb: Long,
    val modifiedAt: Long,
    val manual: Boolean,
)

/** 文件元数据在 IO 线程读取，列表重组和焦点移动不再访问文件系统。 */
private fun loadLogFiles(): List<LogEntry> = runCatching {
    LogCatcherUtil.updateLogFiles()
    (LogCatcherUtil.manualFiles + LogCatcherUtil.crashFiles)
        .map {
            LogEntry(
                file = it,
                sizeKb = it.length() / 1024,
                modifiedAt = it.lastModified(),
                manual = it.name.startsWith("logs_manual"),
            )
        }
        .sortedByDescending { it.modifiedAt }
}.getOrDefault(emptyList())

/**
 * 日志（第 9.11 节模板；逻辑照抄旧电视 LogsScreen）：
 * 「手动保存日志」+ 日志文件列表；选中文件后显示下载二维码（应用内 HttpServer 的 /api/logs/{name}），
 * 同一局域网的设备扫码即可下载。平板 / 电视：右侧常驻二维码面板（电视聚焦即选中）；手机：点文件弹出二维码。
 */
@Composable
fun LogsScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layout = LocalDeviceProfile.current.layout
    val wide = layout != DeviceLayout.Phone
    val tv = layout == DeviceLayout.Tv
    val logs = remember { mutableStateListOf<LogEntry>() }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<File?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val firstRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        val ip = withContext(Dispatchers.IO) { ipAddress(context) }
        val serverPort = runCatching {
            HttpServer.server?.engine?.resolvedConnectors()?.firstOrNull()?.port
        }.getOrNull() ?: 0
        val files = withContext(Dispatchers.IO) { loadLogFiles() }
        host = ip
        port = serverPort
        logs.clear()
        logs.addAll(files)
        if (tv) {
            withFrameNanos { }
            firstRequester.tryToolFocus()
        }
    }

    val saveLog: () -> Unit = {
        if (!saving) {
            saving = true
            scope.launch {
                val files = withContext(Dispatchers.IO) {
                    runCatching { LogCatcherUtil.logLogcat(manual = true) }
                        .onFailure { logsLogger.warn(it) { "save log failed" } }
                    loadLogFiles()
                }
                logs.clear()
                logs.addAll(files)
                saving = false
                Toast.makeText(context, R.string.tools_logs_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val qrContent = selected?.let { "http://$host:$port/api/logs/${it.name}" }.orEmpty()

    ToolPage(title = stringResource(R.string.tools_logs_title), onBack = { nav.popBackStack() }) { insets ->
        if (!wide) {
            LogList(
                logs = logs,
                selected = null,
                insets = insets.padding,
                spacing = 2.dp,
                firstRequester = firstRequester,
                onSave = saveLog,
                onSaveFocused = {},
                onFile = { file ->
                    selected = file
                    showQrDialog = true
                },
                onFileFocused = {},
            )
        } else {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(insets.padding),
                horizontalArrangement = Arrangement.spacedBy(insets.spacing),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    LogList(
                        logs = logs,
                        selected = selected,
                        insets = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
                        spacing = 2.dp,
                        firstRequester = firstRequester,
                        onSave = saveLog,
                        onSaveFocused = { if (tv) selected = null },
                        onFile = { selected = it },
                        onFileFocused = { if (tv) selected = it },
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (tv) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            RoundedCornerShape(28.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    LogQrPanel(qrContent = qrContent, fileName = selected?.name)
                }
            }
        }
    }

    if (showQrDialog && selected != null) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    Modifier
                        .widthIn(min = 280.dp, max = 400.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(R.string.tools_logs_qr_title), style = BvType.PanelTitle)
                    LogQrPanel(qrContent = qrContent, fileName = selected?.name)
                    BvButton(
                        text = stringResource(R.string.tools_close),
                        onClick = { showQrDialog = false },
                        modifier = Modifier.align(Alignment.End),
                        style = BvButtonStyle.Tonal,
                    )
                }
            }
        }
    }
}

@Composable
private fun LogList(
    logs: List<LogEntry>,
    selected: File?,
    insets: PaddingValues,
    spacing: androidx.compose.ui.unit.Dp,
    firstRequester: FocusRequester,
    onSave: () -> Unit,
    onSaveFocused: () -> Unit,
    onFile: (File) -> Unit,
    onFileFocused: (File) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    TvColumnPivot {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .focusRestorer()
                .focusGroup(),
            contentPadding = insets,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            item(key = "save", contentType = "save") {
                BvListItem(
                    title = stringResource(R.string.tools_logs_save),
                    modifier = Modifier.focusRequester(firstRequester),
                    subtitle = stringResource(R.string.tools_logs_save_desc),
                    icon = Icons.Rounded.Save,
                    iconContainerColor = scheme.primaryContainer,
                    iconContentColor = scheme.onPrimaryContainer,
                    onClick = onSave,
                    onFocused = { if (it) onSaveFocused() },
                )
            }
            if (logs.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Text(
                        stringResource(R.string.tools_logs_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        style = BvType.Body,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(
                items = logs,
                key = { it.file.absolutePath },
                contentType = { "log" },
            ) { entry ->
                val manual = entry.manual
                BvListItem(
                    title = entry.file.name,
                    subtitle = stringResource(
                        R.string.tools_logs_item,
                        stringResource(if (manual) R.string.tools_logs_manual else R.string.tools_logs_crash),
                        entry.sizeKb,
                    ),
                    icon = if (manual) Icons.Rounded.Description else Icons.Rounded.BugReport,
                    iconContainerColor = if (manual) scheme.secondaryContainer else scheme.tertiaryContainer,
                    iconContentColor = if (manual) scheme.onSecondaryContainer else scheme.onTertiaryContainer,
                    selected = entry.file == selected,
                    onClick = { onFile(entry.file) },
                    onFocused = { if (it) onFileFocused(entry.file) },
                )
            }
        }
    }
}

/** 二维码面板：白底圆角 24、内边距 14 的 Material 形状二维码 + 文件名；未选择时显示提示。 */
@Composable
private fun LogQrPanel(qrContent: String, fileName: String?) {
    Column(
        Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (qrContent.isNotBlank()) {
            Box(
                Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) {
                MaterialShapeQr(modifier = Modifier.fillMaxSize(), content = qrContent)
            }
            if (fileName != null) {
                Text(
                    fileName,
                    style = BvType.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            stringResource(R.string.tools_logs_qr_hint),
            style = BvType.Small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
