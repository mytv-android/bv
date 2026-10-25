package dev.aaa1115910.bv.ui.designsystem.adaptive

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import dev.aaa1115910.bv.entity.LayoutMode
import dev.aaa1115910.bv.util.Prefs

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
    /**
     * 实际生效的界面布局，由 [rememberDeviceProfile] 解析：
     * 用户在设置里强制指定时用指定值，否则 Compact/Medium 用手机布局、
     * Expanded 及以上用平板布局，电视用电视布局。
     */
    val layout: DeviceLayout,
) {
    val isTv: Boolean get() = distance == Distance.Far
    val navKind: NavKind
        get() = when {
            isTv -> NavKind.TvRail
            width >= WidthClass.Large -> NavKind.RailExpanded
            width >= WidthClass.Medium -> NavKind.Rail
            else -> NavKind.Bar
        }
}

val LocalDeviceProfile = staticCompositionLocalOf {
    DeviceProfile(WidthClass.Compact, BvInputMode.Touch, Distance.Near, DeviceLayout.Phone)
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
    // 设置 → 外观与主题 → 界面布局：改了立刻整个 App 换布局，不用重启
    val modeFlow = remember { Prefs.layoutModeFlow }
    val mode by modeFlow.collectAsStateWithLifecycle(initialValue = LayoutMode.Auto)
    val width = when {
        widthDp < 600 -> WidthClass.Compact
        widthDp < 840 -> WidthClass.Medium
        widthDp < 1200 -> WidthClass.Expanded
        else -> WidthClass.Large
    }
    // 强制「电视横屏」→ 按电视对待（方向键 + 远距 + 电视密度）；
    // 强制手机 / 平板 → 近距，输入方式仍看真实设备（电视上没有触摸屏，仍走方向键）
    val distance = when (mode) {
        LayoutMode.Tv -> Distance.Far
        LayoutMode.Phone, LayoutMode.Tablet -> Distance.Near
        LayoutMode.Auto -> if (tv) Distance.Far else Distance.Near
    }
    val input = if (tv || keyboard || mode == LayoutMode.Tv) BvInputMode.DPad else BvInputMode.Touch
    val layout = when (mode) {
        LayoutMode.Auto -> when {
            tv -> DeviceLayout.Tv
            width >= WidthClass.Expanded -> DeviceLayout.Tablet
            else -> DeviceLayout.Phone
        }

        LayoutMode.Phone -> DeviceLayout.Phone
        LayoutMode.Tablet -> DeviceLayout.Tablet
        LayoutMode.Tv -> DeviceLayout.Tv
    }
    return remember(width, input, distance, layout) {
        DeviceProfile(width, input, distance, layout)
    }
}
