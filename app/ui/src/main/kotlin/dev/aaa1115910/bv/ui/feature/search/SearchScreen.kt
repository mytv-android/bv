package dev.aaa1115910.bv.ui.feature.search

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.navigation.SearchRoute

/**
 * 搜索（第 9.5 节，原型 searchPhone / searchTablet / searchTv）。
 * SearchInputViewModel（联想、热搜、历史）与 SearchResultViewModel（视频 / 番剧 / 影视 / UP 主四类结果）
 * 在这里创建，三种布局共享。route.query 不为空时直接打开结果。
 */
@Composable
fun SearchScreen(nav: NavController, route: SearchRoute) {
    val state = rememberSearchUiState(route)
    val follow = rememberFollowState(state.apiType)
    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> SearchPhone(nav, state, follow)
        DeviceLayout.Tablet -> SearchTablet(nav, state, follow)
        DeviceLayout.Tv -> SearchTv(nav, state, follow, initialFull = route.query.isNotBlank())
    }
}
