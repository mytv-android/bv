package dev.aaa1115910.bv.ui.feature.login

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import dev.aaa1115910.bv.entity.BvScheme

/**
 * 处理 bugvideo://qrtoken 深链。
 * TODO(redesign 步骤 8)：改为新界面里的「导入账号」对话框；目前转交旧的 QrTokenResultActivity。
 */
@Composable
fun DeepLinkEffect(intent: Intent?, nav: NavController) {
    val context = LocalContext.current
    LaunchedEffect(intent) {
        val uri = intent?.data ?: return@LaunchedEffect
        if (uri.host == BvScheme.QrToken.HOST) {
            runCatching {
                context.startActivity(
                    Intent()
                        .setClassName(context, "dev.aaa1115910.bv.mobile.activities.QrTokenResultActivity")
                        .putExtra("uri", uri)
                )
            }
        }
    }
}
