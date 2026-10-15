@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.aaa1115910.bv.ui.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.aaa1115910.bv.entity.DarkMode
import dev.aaa1115910.bv.entity.ThemePrefs
import dev.aaa1115910.bv.entity.ThemeSource
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.adaptive.rememberDeviceProfile
import dev.aaa1115910.bv.util.Prefs

val LocalThemePrefs = staticCompositionLocalOf { ThemePrefs() }
val LocalBvDark = staticCompositionLocalOf { false }

/** 应用根：提供设备画像、主题偏好、电视密度，然后套全局主题。只在 MainActivity.setContent 里调用一次。 */
@Composable
fun BvRoot(initialPrefs: ThemePrefs, content: @Composable () -> Unit) {
    val prefs by Prefs.themePrefsFlow.collectAsState(initial = initialPrefs)
    val profile = rememberDeviceProfile()
    val context = LocalContext.current
    val base = LocalDensity.current
    val tvDensity by Prefs.densityFlow.collectAsState(initial = context.resources.displayMetrics.widthPixels / 960f)
    val density = if (profile.isTv) Density(tvDensity, base.fontScale) else base
    CompositionLocalProvider(
        LocalDeviceProfile provides profile,
        LocalThemePrefs provides prefs,
        LocalDensity provides density,
    ) {
        BvTheme(content = content)
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
            content = content,
        )
    }
}
