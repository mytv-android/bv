package dev.aaa1115910.bv.ui.designsystem.adaptive

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager

enum class WidthClass { Compact, Medium, Expanded, Large }
enum class BvInputMode { Touch, DPad }
enum class Distance { Near, Far }
enum class NavKind { Bar, Rail, RailExpanded, TvRail }
enum class DeviceLayout { Phone, Tablet, Tv }

@Immutable
data class DeviceProfile(
    val width: WidthClass,
    val input: BvInputMode,
    val distance: Distance,
) {
    val isTv: Boolean get() = distance == Distance.Far
    val navKind: NavKind
        get() = when {
            isTv -> NavKind.TvRail
            width >= WidthClass.Large -> NavKind.RailExpanded
            width >= WidthClass.Medium -> NavKind.Rail
            else -> NavKind.Bar
        }

    /** 界面布局：Compact/Medium 用手机布局，Expanded 及以上用平板布局，电视用电视布局 */
    val layout: DeviceLayout
        get() = when {
            isTv -> DeviceLayout.Tv
            width >= WidthClass.Expanded -> DeviceLayout.Tablet
            else -> DeviceLayout.Phone
        }
}

val LocalDeviceProfile = staticCompositionLocalOf {
    DeviceProfile(WidthClass.Compact, BvInputMode.Touch, Distance.Near)
}

fun Context.isTelevision(): Boolean {
    val ui = getSystemService(UiModeManager::class.java)
    return ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

@Composable
fun rememberDeviceProfile(): DeviceProfile {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val tv = remember(context) { context.isTelevision() }
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val width = when {
        widthDp < 600 -> WidthClass.Compact
        widthDp < 840 -> WidthClass.Medium
        widthDp < 1200 -> WidthClass.Expanded
        else -> WidthClass.Large
    }
    val input = if (tv || keyboard) BvInputMode.DPad else BvInputMode.Touch
    return remember(width, input, tv) {
        DeviceProfile(width, input, if (tv) Distance.Far else Distance.Near)
    }
}
