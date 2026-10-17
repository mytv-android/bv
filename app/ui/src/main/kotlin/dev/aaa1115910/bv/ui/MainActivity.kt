package dev.aaa1115910.bv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dev.aaa1115910.bv.ui.designsystem.theme.BvRoot
import dev.aaa1115910.bv.ui.feature.accounts.UserLockGate
import dev.aaa1115910.bv.ui.feature.login.DeepLinkEffect
import dev.aaa1115910.bv.ui.navigation.AppShell
import dev.aaa1115910.bv.util.CacheManager
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** 唯一启动入口：同时声明 LAUNCHER 与 LEANBACK_LAUNCHER。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 启动时后台检查缓存阈值（默认「不限制」，只有用户设置上限后才会清理）
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { CacheManager(applicationContext).checkCache() }
        }
        val initialPrefs = runBlocking { Prefs.themePrefsFlow.first() }   // 只读一次，避免首帧主题闪烁
        setContent {
            BvRoot(initialPrefs) {
                UserLockGate {
                    val nav = rememberNavController()
                    AppShell(nav)
                    DeepLinkEffect(intent, nav)   // 处理 bugvideo://qrtoken
                }
            }
        }
    }
}
