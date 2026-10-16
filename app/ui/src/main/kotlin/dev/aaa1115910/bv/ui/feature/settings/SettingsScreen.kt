package dev.aaa1115910.bv.ui.feature.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.navigation.SettingsRoute

/** 分类图标容器的颜色角色（第 9.9 节表格：primary / tertiary / secondary 轮换）。 */
internal enum class CategoryRole { Primary, Secondary, Tertiary }

/** 第 9.9 节的 8 个分类：顺序、图标、图标容器颜色固定。id 与 [SettingsRoute.category] 对应。 */
internal enum class SettingsCategory(
    val id: String,
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
    val icon: () -> ImageVector,
    val role: CategoryRole,
) {
    Appearance("appearance", R.string.settings_cat_appearance, R.string.settings_cat_appearance_desc, { Icons.Rounded.Palette }, CategoryRole.Primary),
    Play("play", R.string.settings_cat_play, R.string.settings_cat_play_desc, { Icons.Rounded.PlayArrow }, CategoryRole.Tertiary),
    Danmaku("danmaku", R.string.settings_cat_danmaku, R.string.settings_cat_danmaku_desc, { Icons.Rounded.ChatBubble }, CategoryRole.Secondary),
    Subtitle("subtitle", R.string.settings_cat_subtitle, R.string.settings_cat_subtitle_desc, { Icons.Rounded.Subtitles }, CategoryRole.Primary),
    Network("network", R.string.settings_cat_network, R.string.settings_cat_network_desc, { Icons.Rounded.Wifi }, CategoryRole.Tertiary),
    Account("account", R.string.settings_cat_account, R.string.settings_cat_account_desc, { Icons.Rounded.Shield }, CategoryRole.Secondary),
    Storage("storage", R.string.settings_cat_storage, R.string.settings_cat_storage_desc, { Icons.Rounded.Storage }, CategoryRole.Primary),
    About("about", R.string.settings_cat_about, R.string.settings_cat_about_desc, { Icons.Rounded.Info }, CategoryRole.Tertiary);

    companion object {
        fun of(id: String): SettingsCategory? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 设置（第 9.9 节，原型 setPhone / setTablet / setTv）。
 * - 手机：category 为空 → 分类列表；否则 → 该分类的详情页（LargeFlexibleTopAppBar）。
 * - 平板 / 电视：列表-详情两栏，默认选中 category（为空时「外观与主题」）。
 */
@Composable
fun SettingsScreen(nav: NavController, route: SettingsRoute) {
    val category = SettingsCategory.of(route.category)
    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone ->
            if (category == null) SettingsPhoneList(nav) else SettingsPhoneDetail(nav, category)

        DeviceLayout.Tablet -> SettingsTablet(nav, category ?: SettingsCategory.Appearance)
        DeviceLayout.Tv -> SettingsTv(nav, category ?: SettingsCategory.Appearance)
    }
}
