package com.github.mytv.bv.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.mytv.bv.ui.PreferenceWrites
import com.github.mytv.bv.ui.designsystem.component.FullPageLoading
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.adaptive.NavKind
import com.github.mytv.bv.ui.feature.accounts.AccountsScreen
import com.github.mytv.bv.ui.feature.accounts.UserLockScreen
import com.github.mytv.bv.ui.feature.dynamic.DynamicDetailScreen
import com.github.mytv.bv.ui.feature.dynamic.DynamicScreen
import com.github.mytv.bv.ui.feature.favorite.FavoriteScreen
import com.github.mytv.bv.ui.feature.following.FollowingScreen
import com.github.mytv.bv.ui.feature.following.FollowingSeasonScreen
import com.github.mytv.bv.ui.feature.history.HistoryScreen
import com.github.mytv.bv.ui.feature.home.HomeScreen
import com.github.mytv.bv.ui.feature.live.LivePlayerScreen
import com.github.mytv.bv.ui.feature.live.LiveScreen
import com.github.mytv.bv.ui.feature.login.LoginScreen
import com.github.mytv.bv.ui.feature.me.MeScreen
import com.github.mytv.bv.ui.feature.pgc.PgcScreen
import com.github.mytv.bv.ui.feature.pgcindex.PgcIndexScreen
import com.github.mytv.bv.ui.feature.player.PlayerScreen
import com.github.mytv.bv.ui.feature.search.SearchScreen
import com.github.mytv.bv.ui.feature.settings.SettingsScreen
import com.github.mytv.bv.ui.feature.shorts.ShortsScreen
import com.github.mytv.bv.ui.feature.space.SpaceScreen
import com.github.mytv.bv.ui.feature.tag.TagScreen
import com.github.mytv.bv.ui.feature.timeline.PgcTimelineScreen
import com.github.mytv.bv.ui.feature.tools.LogsScreen
import com.github.mytv.bv.ui.feature.tools.MediaCodecScreen
import com.github.mytv.bv.ui.feature.tools.SpeedTestScreen
import com.github.mytv.bv.ui.feature.toview.ToViewScreen
import com.github.mytv.bv.ui.feature.video.SeasonScreen
import com.github.mytv.bv.ui.feature.video.VideoDetailScreen
import kotlin.math.sign

/**
 * 全部路由。页面切换：fadeIn(defaultEffectsSpec) + 按导航顺序方向的位移
 * （底部导航栏横向、导航轨纵向，沿用旧 MobileMainScreen 的做法）；非一级页面从右侧进入。
 */
/**
 * 每个页面的根节点：一个铺满的 Box。
 * 页面在根部并列多个组合（例如页面 + 包在另一层 BvTheme / CoverTheme 里的对话框、底部抽屉）时，
 * 直接放在 NavHost 的 AnimatedContent 里会导致前面的内容整个不显示；套一层 Box 后正常。
 */
@Composable
private fun Screen(content: @Composable () -> Unit) {
    // Wait after apply as well: outgoing effects may enqueue their final save
    // in this commit. Saves accepted later still belong to that outgoing screen.
    var preferencesReady by remember { mutableStateOf(false) }
    if (!preferencesReady) {
        LaunchedEffect(Unit) {
            PreferenceWrites.awaitPending()
            preferencesReady = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (preferencesReady) content() else FullPageLoading()
    }
}

@Composable
fun BvNavHost(nav: NavHostController, modifier: Modifier = Modifier) {
    val horizontal = LocalDeviceProfile.current.navKind == NavKind.Bar
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

    fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(): Int {
        val from = TopLevel.of(initialState.destination)
        val to = TopLevel.of(targetState.destination)
        return if (from != null && to != null) (to.ordinal - from.ordinal).sign else 0
    }

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        val dir = direction()
        when {
            dir == 0 -> fadeIn(effects) + slideInHorizontally(spatial) { it / 10 }
            horizontal -> fadeIn(effects) + slideInHorizontally(spatial) { dir * it / 10 }
            else -> fadeIn(effects) + slideInVertically(spatial) { dir * it / 10 }
        }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        val dir = direction()
        when {
            dir == 0 -> fadeOut(effects)
            horizontal -> fadeOut(effects) + slideOutHorizontally(spatial) { -dir * it / 10 }
            else -> fadeOut(effects) + slideOutVertically(spatial) { -dir * it / 10 }
        }
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { fadeIn(effects) }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(effects) + slideOutHorizontally(spatial) { it / 10 }
    }

    NavHost(
        navController = nav,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit,
    ) {
        composable<HomeRoute> { Screen { HomeScreen(nav) } }
        composable<ShortsRoute> { Screen { ShortsScreen(nav, it.toRoute()) } }
        composable<PgcRoute> { Screen { PgcScreen(nav) } }
        composable<LiveRoute> { Screen { LiveScreen(nav) } }
        composable<LivePlayerRoute> { Screen { LivePlayerScreen(nav, it.toRoute()) } }
        composable<DynamicRoute> { Screen { DynamicScreen(nav) } }
        composable<MeRoute> { Screen { MeScreen(nav) } }
        composable<SearchRoute> { Screen { SearchScreen(nav, it.toRoute()) } }
        composable<VideoRoute> { Screen { VideoDetailScreen(nav, it.toRoute()) } }
        composable<SeasonRoute> { Screen { SeasonScreen(nav, it.toRoute()) } }
        composable<PlayerRoute> { Screen { PlayerScreen(nav, it.toRoute()) } }
        composable<SpaceRoute> { Screen { SpaceScreen(nav, it.toRoute()) } }
        composable<TagRoute> { Screen { TagScreen(nav, it.toRoute()) } }
        composable<DynamicDetailRoute> { Screen { DynamicDetailScreen(nav, it.toRoute()) } }
        composable<PgcIndexRoute> { Screen { PgcIndexScreen(nav, it.toRoute()) } }
        composable<PgcTimelineRoute> { Screen { PgcTimelineScreen(nav) } }
        composable<HistoryRoute> { Screen { HistoryScreen(nav) } }
        composable<FavoriteRoute> { Screen { FavoriteScreen(nav) } }
        composable<ToViewRoute> { Screen { ToViewScreen(nav) } }
        composable<FollowingSeasonRoute> { Screen { FollowingSeasonScreen(nav) } }
        composable<FollowingRoute> { Screen { FollowingScreen(nav) } }
        composable<AccountsRoute> { Screen { AccountsScreen(nav) } }
        composable<UserLockRoute> { Screen { UserLockScreen(nav) } }
        composable<LoginRoute> { Screen { LoginScreen(nav) } }
        composable<SettingsRoute> { Screen { SettingsScreen(nav, it.toRoute()) } }
        composable<LogsRoute> { Screen { LogsScreen(nav) } }
        composable<MediaCodecRoute> { Screen { MediaCodecScreen(nav) } }
        composable<SpeedTestRoute> { Screen { SpeedTestScreen(nav) } }
    }
}
