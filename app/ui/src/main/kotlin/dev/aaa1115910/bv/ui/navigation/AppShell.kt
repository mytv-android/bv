package dev.aaa1115910.bv.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.adaptive.NavKind
import dev.aaa1115910.bv.ui.designsystem.component.LocalVideoCardActions
import dev.aaa1115910.bv.ui.designsystem.component.VideoCardActions
import dev.aaa1115910.bv.ui.designsystem.nav.PhoneNavBar
import dev.aaa1115910.bv.ui.designsystem.nav.TabletRail
import dev.aaa1115910.bv.ui.designsystem.nav.TvRail
import dev.aaa1115910.bv.ui.designsystem.nav.TvRailTarget
import dev.aaa1115910.bv.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** 手机布局下一级页面滚动内容底部需要留出的导航栏高度（64dp + 系统导航栏 inset）。 */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

/**
 * 应用外壳（第 8.3 节）：
 * - Bar：NavHost 铺满，一级页面时 PhoneNavBar 叠在底部；
 * - Rail / RailExpanded：TabletRail + NavHost，非一级页面隐藏导航轨（搜索页显示，不选中任何项）；
 * - TvRail：NavHost 铺满，一级页面与搜索页时 TvRail 叠在左侧。
 */
@Composable
fun AppShell(nav: NavHostController) {
    val profile = LocalDeviceProfile.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toViewRepository: ToViewRepository = koinInject()
    val onAddToView: (Long) -> Unit = { aid ->
        scope.launch {
            val ok = runCatching { toViewRepository.addToView(aid) }.isSuccess
            Toast.makeText(
                context,
                if (ok) R.string.shell_toview_added else R.string.shell_toview_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val current = TopLevel.of(destination)
    val onSearchPage = destination?.hasRoute(SearchRoute::class) == true

    val actions = remember(nav) {
        object : VideoCardActions {
            override fun openShorts(aid: Long) = nav.navigate(ShortsRoute(startAid = aid))
            override fun addToView(aid: Long) = onAddToView(aid)
            override fun openUp(mid: Long) = nav.navigate(SpaceRoute(mid))
        }
    }

    // 手机横竖屏切换会在 Bar / Rail 两种外壳之间切换：NavHost 用 movableContentOf 原地移动，
    // 页面（含正在播放的播放器）不会被销毁重建（第 14.3 节「布局切换且正在播放的视频不中断」）
    val navHost = remember(nav) {
        movableContentOf { modifier: Modifier -> BvNavHost(nav, modifier) }
    }

    CompositionLocalProvider(
        LocalVideoCardActions provides actions,
        // 原型尺寸精确到 dp，不让 M3 把小按钮的布局撑到 48dp
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
    ) {
        when (profile.navKind) {
            NavKind.Bar -> Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val barPadding = 64.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                CompositionLocalProvider(LocalBottomBarPadding provides if (current != null) barPadding else 0.dp) {
                    navHost(Modifier.fillMaxSize())
                }
                if (current != null) {
                    PhoneNavBar(
                        current = current,
                        onSelect = nav::navigateTopLevel,
                        overVideo = current == TopLevel.Shorts,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            NavKind.Rail, NavKind.RailExpanded -> Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (current != null || onSearchPage) {
                    TabletRail(
                        current = current,
                        expanded = profile.navKind == NavKind.RailExpanded,
                        onSelect = nav::navigateTopLevel,
                        onSearch = { nav.navigate(SearchRoute()) { launchSingleTop = true } },
                        onSettings = { nav.navigate(SettingsRoute()) },
                    )
                }
                navHost(Modifier.weight(1f))
            }

            NavKind.TvRail -> TvShell(nav, current, onSearchPage, context as? Activity)
        }
    }
}

@Composable
private fun TvShell(nav: NavHostController, current: TopLevel?, onSearchPage: Boolean, activity: Activity?) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = koinViewModel()
    val railRequester = remember { FocusRequester() }
    var railFocused by remember { mutableStateOf(false) }
    var lastBack by remember { mutableLongStateOf(0L) }
    val showRail = current != null || onSearchPage

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        BvNavHost(nav, Modifier.fillMaxSize())
        // 必须在 NavHost 之后注册，才能先于 NavHost 自己的返回处理
        // 电视返回键（一级页面）：焦点不在导航轨 → 移到导航轨当前项；已在导航轨 → 3 秒内再按一次退出
        BackHandler(enabled = current != null) {
            if (!railFocused) {
                runCatching { railRequester.requestFocus() }
            } else {
                val now = System.currentTimeMillis()
                if (now - lastBack < 3000) {
                    activity?.finish()
                } else {
                    lastBack = now
                    Toast.makeText(context, R.string.shell_press_back_again_exit, Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (showRail) {
            TvRail(
                current = if (onSearchPage) TvRailTarget.Search else current?.let { TvRailTarget.Page(it) },
                avatar = userViewModel.face.takeIf { it.isNotBlank() },
                userName = userViewModel.username,
                onSelect = nav::navigateTopLevel,
                onSearch = { nav.navigate(SearchRoute()) { launchSingleTop = true } },
                onSettings = { nav.navigate(SettingsRoute()) },
                selectedRequester = railRequester,
                onFocusChanged = { railFocused = it },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}
