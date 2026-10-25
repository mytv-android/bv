@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.aaa1115910.bv.ui.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.aaa1115910.bv.component.FpsMonitor
import dev.aaa1115910.bv.entity.DarkMode
import dev.aaa1115910.bv.entity.ThemePrefs
import dev.aaa1115910.bv.entity.ThemeSource
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.adaptive.rememberDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.focus.LocalFocusColor
import dev.aaa1115910.bv.util.Prefs

val LocalThemePrefs = staticCompositionLocalOf { ThemePrefs() }
val LocalBvDark = staticCompositionLocalOf { false }

/** 应用根：提供设备画像、主题偏好、电视密度，然后套全局主题。只在 MainActivity.setContent 里调用一次。 */
@Composable
fun BvRoot(initialPrefs: ThemePrefs, initialShowFps: Boolean, content: @Composable () -> Unit) {
    val prefs by Prefs.themePrefsFlow.collectAsStateWithLifecycle(initialValue = initialPrefs)
    val profile = rememberDeviceProfile()
    val context = LocalContext.current
    val base = LocalDensity.current
    val tvDensity by Prefs.densityFlow.collectAsStateWithLifecycle(initialValue = context.resources.displayMetrics.widthPixels / 960f)
    val density = if (profile.isTv) Density(tvDensity, base.fontScale) else base
    val showFps = initialShowFps
    CompositionLocalProvider(
        LocalDeviceProfile provides profile,
        LocalThemePrefs provides prefs,
        LocalDensity provides density,
    ) {
        BvTheme {
            // 设置「关于」里的「显示 FPS」（沿用旧 BVTheme 的做法，启动时读一次）
            if (showFps) FpsMonitor(content = content) else content()
        }
    }
}

/** 全局主题。forceDark = true 用于播放器、刷刷（原型里的 .cinema）。电视永远深色。 */
@Composable
fun BvTheme(forceDark: Boolean = false, content: @Composable () -> Unit) {
    val prefs = LocalThemePrefs.current
    val profile = LocalDeviceProfile.current
    val systemDark = isSystemInDarkTheme()
    val dark = forceDark || profile.isTv || when (prefs.darkMode) {
        DarkMode.System -> systemDark
        DarkMode.Light -> false
        DarkMode.Dark, DarkMode.PureBlack -> true
    }
    val context = LocalContext.current
    val seed = when {
        prefs.source == ThemeSource.Wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            remember(context) { dynamicLightColorScheme(context).primary }

        prefs.source == ThemeSource.Content -> prefs.contentSeed ?: prefs.seed
        else -> prefs.seed
    }
    if (!profile.isTv) SystemBarsAppearance(dark)
    CompositionLocalProvider(LocalBvDark provides dark) {
        DynamicMaterialExpressiveTheme(
            seedColor = seed,
            motionScheme = if (prefs.reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            isDark = dark,
            isAmoled = dark && prefs.darkMode == DarkMode.PureBlack,
            style = prefs.style,
            contrastLevel = prefs.contrast,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            animate = true,
            content = {
                CompositionLocalProvider(
                    LocalFocusColor provides MaterialTheme.colorScheme.primary,
                ) {
                    content()
                }
            },
        )
    }
}

/**
 * 状态栏 / 导航栏图标颜色跟随 App 自己的明暗（enableEdgeToEdge 默认只跟随系统深色模式）。
 * 嵌套的 BvTheme(forceDark = true)（播放器、刷刷）离开时恢复外层的设置。
 */
@Composable
private fun SystemBarsAppearance(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, dark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !dark
        controller?.isAppearanceLightNavigationBars = !dark
        onDispose {
            previousStatus?.let { controller.isAppearanceLightStatusBars = it }
            previousNavigation?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }
}
