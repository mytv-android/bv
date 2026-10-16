package dev.aaa1115910.bv.ui.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.repositories.ChannelRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.Prefs
import org.koin.compose.koinInject

private enum class ProxyField { Http, Grpc }

/**
 * 「网络与代理」：代理开关 + HTTP / gRPC 代理服务器、官方 CDN、接口类型（Web / App）、网络测试入口。
 * 逻辑照抄旧电视 NetworkSetting 与 ApiSetting。
 */
@Composable
internal fun NetworkSettings(onSpeedTest: () -> Unit) {
    val channelRepository: ChannelRepository = koinInject()
    var enableProxy by remember { mutableStateOf(Prefs.enableProxy) }
    var proxyHttpServer by remember { mutableStateOf(Prefs.proxyHttpServer) }
    var proxyGrpcServer by remember { mutableStateOf(Prefs.proxyGRPCServer) }
    var preferOfficialCdn by remember { mutableStateOf(Prefs.preferOfficialCdn) }
    var apiType by remember { mutableStateOf(Prefs.apiType) }
    var editing by remember { mutableStateOf<ProxyField?>(null) }
    val emptyText = stringResource(R.string.settings_net_proxy_empty)

    Column(Modifier.fillMaxWidth()) {
        SettingSwitchRow(
            title = stringResource(R.string.settings_net_proxy),
            subtitle = stringResource(R.string.settings_net_proxy_desc),
            checked = enableProxy,
            onCheckedChange = { enable ->
                enableProxy = enable
                Prefs.enableProxy = enable
                if (enable) BVApp.instance?.initProxy()
            },
        )
        AnimatedVisibility(visible = enableProxy) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SettingNavRow(
                    title = stringResource(R.string.settings_net_proxy_http),
                    subtitle = proxyHttpServer.ifBlank { emptyText },
                    onClick = { editing = ProxyField.Http },
                )
                SettingNavRow(
                    title = stringResource(R.string.settings_net_proxy_grpc),
                    subtitle = proxyGrpcServer.ifBlank { emptyText },
                    onClick = { editing = ProxyField.Grpc },
                )
            }
        }
    }

    SettingSwitchRow(
        title = stringResource(R.string.settings_net_cdn),
        subtitle = stringResource(R.string.settings_net_cdn_desc),
        checked = preferOfficialCdn,
        onCheckedChange = {
            preferOfficialCdn = it
            Prefs.preferOfficialCdn = it
        },
    )

    SettingChoiceGroup(
        label = stringResource(R.string.settings_net_api),
        options = ApiType.entries.toList(),
        isSelected = { it == apiType },
        optionLabel = { it.name },
        onSelect = {
            apiType = it
            Prefs.apiType = it
        },
    )

    SettingRows(stringResource(R.string.settings_net_tools)) {
        SettingNavRow(
            title = stringResource(R.string.settings_net_speed_test),
            subtitle = stringResource(R.string.settings_net_speed_test_desc),
            onClick = onSpeedTest,
        )
    }

    when (editing) {
        ProxyField.Http -> ProxyServerDialog(
            title = stringResource(R.string.settings_net_proxy_http),
            server = proxyHttpServer,
            onDismiss = { editing = null },
            onConfirm = { server ->
                editing = null
                proxyHttpServer = server
                Prefs.proxyHttpServer = server
                runCatching { BiliHttpProxyApi.createClient(server) }
            },
        )

        ProxyField.Grpc -> ProxyServerDialog(
            title = stringResource(R.string.settings_net_proxy_grpc),
            server = proxyGrpcServer,
            onDismiss = { editing = null },
            onConfirm = { server ->
                editing = null
                proxyGrpcServer = server
                Prefs.proxyGRPCServer = server
                runCatching {
                    channelRepository.initProxyChannel(
                        accessKey = Prefs.accessToken,
                        buvid = Prefs.buvid,
                        proxyServer = server,
                    )
                }
            },
        )

        null -> Unit
    }
}

/** 编辑代理服务器（旧 ProxyServerEditDialog）：去掉换行与 http(s):// 前缀后保存。 */
@Composable
private fun ProxyServerDialog(
    title: String,
    server: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by remember { mutableStateOf(server) }
    val fieldRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { fieldRequester.requestFocus() }
    }
    SettingsDialog(onDismiss) {
        Text(title, style = BvType.PanelTitle)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fieldRequester),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(stringResource(R.string.settings_net_proxy_hint)) },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.settings_net_proxy_warning),
                style = BvType.Label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsDialogActions(
            confirm = stringResource(R.string.settings_confirm),
            onConfirm = {
                onConfirm(
                    input
                        .replace("\n", "")
                        .replace("https://", "")
                        .replace("http://", "")
                )
            },
            dismiss = stringResource(R.string.settings_cancel),
            onDismiss = onDismiss,
        )
    }
}
