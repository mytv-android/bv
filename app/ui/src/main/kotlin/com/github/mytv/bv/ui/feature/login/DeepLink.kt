package com.github.mytv.bv.ui.feature.login

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.github.mytv.bv.entity.BvScheme

/**
 * 处理 bugvideo://qrtoken 深链（第 8.1 节）：在新界面里弹出「导入账号」对话框（移植旧 QrTokenResultScreen）。
 * 每个 Intent 只处理一次（rememberSaveable 记住已处理过）。
 */
@Composable
fun DeepLinkEffect(intent: Intent?, nav: NavController) {
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var handled by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(intent) {
        val uri = intent?.data ?: return@LaunchedEffect
        val text = uri.toString()
        if (uri.host == BvScheme.QrToken.HOST && handled != text) {
            handled = text
            pending = text
        }
    }
    pending?.let { text ->
        QrTokenImportDialog(uri = Uri.parse(text), onDismiss = { pending = null })
    }
}
