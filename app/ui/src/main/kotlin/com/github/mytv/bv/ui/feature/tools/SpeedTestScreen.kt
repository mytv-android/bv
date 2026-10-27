package com.github.mytv.bv.ui.feature.tools

import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.webkit.WebViewClientCompat
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.FullPageLoading
import com.github.mytv.bv.util.Prefs

private const val SPEED_TEST_URL = "https://www.bilibili.com/blackboard/video-diagnostics.html"
private const val SPEED_TEST_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36"

/**
 * 网络测速（第 9.11 节模板；逻辑照抄旧电视 SpeedTestScreen）：
 * 带上登录 Cookie 打开哔哩哔哩视频诊断页，页面加载完后注入 CSS 固定 1920×1080 并按宽度缩放。
 */
@Composable
fun SpeedTestScreen(nav: NavController) {
    var loading by remember { mutableStateOf(true) }
    var cookies by remember { mutableStateOf<Map<String, String>?>(null) }
    LaunchedEffect(Unit) {
        cookies = withContext(Dispatchers.IO) {
            mapOf(
                "DedeUserID" to Prefs.uid.toString(),
                "DedeUserID__ckMd5" to Prefs.uidCkMd5,
                "SESSDATA" to Prefs.sessData,
                "bili_jct" to Prefs.biliJct,
                "sid" to Prefs.sid,
            )
        }
    }
    ToolPage(title = stringResource(R.string.tools_speed_title), onBack = { nav.popBackStack() }) { insets ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(insets.padding)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx().toInt() }
            val loadedCookies = cookies
            if (loadedCookies != null) AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    CookieManager.getInstance().apply {
                        loadedCookies.forEach { (name, value) -> setCookie(".bilibili.com", "$name=$value") }
                    }
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClientCompat() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // 默认的 css 会无法正常显示
                                val css = """
                                    .container {
                                        width: 1920px !important;
                                        height: 1080px !important;
                                    }
                                """.trimIndent()
                                val encoded = Base64.encodeToString(css.toByteArray(), Base64.NO_WRAP)
                                view?.loadUrl(
                                    "javascript:(function() {" +
                                        "var parent = document.getElementsByTagName('head').item(0);" +
                                        "var style = document.createElement('style');" +
                                        "style.type = 'text/css';" +
                                        "style.innerHTML = window.atob('" + encoded + "');" +
                                        "parent.appendChild(style)" +
                                        "})()"
                                )
                                // 处理完 css 还得处理缩放
                                view?.setInitialScale(((widthPx / 1920f) * 100).toInt())
                                loading = false
                            }
                        }
                        WebView.setWebContentsDebuggingEnabled(true)
                        settings.userAgentString = SPEED_TEST_UA
                        settings.javaScriptEnabled = true
                        loadUrl(SPEED_TEST_URL)
                    }
                },
                onRelease = { it.destroy() },
            )
            if (loading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                ) { FullPageLoading() }
            }
        }
    }
}
