package dev.aaa1115910.bv.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.adaptive.NavKind
import dev.aaa1115910.bv.ui.feature.accounts.AccountsScreen
import dev.aaa1115910.bv.ui.feature.accounts.UserLockScreen
import dev.aaa1115910.bv.ui.feature.dynamic.DynamicDetailScreen
import dev.aaa1115910.bv.ui.feature.dynamic.DynamicScreen
import dev.aaa1115910.bv.ui.feature.favorite.FavoriteScreen
import dev.aaa1115910.bv.ui.feature.following.FollowingScreen
import dev.aaa1115910.bv.ui.feature.following.FollowingSeasonScreen
import dev.aaa1115910.bv.ui.feature.history.HistoryScreen
import dev.aaa1115910.bv.ui.feature.home.HomeScreen
import dev.aaa1115910.bv.ui.feature.login.LoginScreen
import dev.aaa1115910.bv.ui.feature.me.MeScreen
import dev.aaa1115910.bv.ui.feature.pgc.PgcScreen
import dev.aaa1115910.bv.ui.feature.pgcindex.PgcIndexScreen
import dev.aaa1115910.bv.ui.feature.player.PlayerScreen
import dev.aaa1115910.bv.ui.feature.search.SearchScreen
import dev.aaa1115910.bv.ui.feature.settings.SettingsScreen
import dev.aaa1115910.bv.ui.feature.shorts.ShortsScreen
import dev.aaa1115910.bv.ui.feature.space.SpaceScreen
import dev.aaa1115910.bv.ui.feature.tag.TagScreen
import dev.aaa1115910.bv.ui.feature.timeline.PgcTimelineScreen
import dev.aaa1115910.bv.ui.feature.tools.LogsScreen
import dev.aaa1115910.bv.ui.feature.tools.MediaCodecScreen
import dev.aaa1115910.bv.ui.feature.tools.SpeedTestScreen
import dev.aaa1115910.bv.ui.feature.toview.ToViewScreen
import dev.aaa1115910.bv.ui.feature.video.SeasonScreen
import dev.aaa1115910.bv.ui.feature.video.VideoDetailScreen
import kotlin.math.sign

/**
 * 全部路由。页面切换：fadeIn(defaultEffectsSpec) + 按导航顺序方向的位移
 * （底部导航栏横向、导航轨纵向，沿用旧 MobileMainScreen 的做法）；非一级页面从右侧进入。
 */
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
        composable<HomeRoute> { HomeScreen(nav) }
        composable<ShortsRoute> { ShortsScreen(nav, it.toRoute()) }
        composable<PgcRoute> { PgcScreen(nav) }
        composable<DynamicRoute> { DynamicScreen(nav) }
        composable<MeRoute> { MeScreen(nav) }
        composable<SearchRoute> { SearchScreen(nav, it.toRoute()) }
        composable<VideoRoute> { VideoDetailScreen(nav, it.toRoute()) }
        composable<SeasonRoute> { SeasonScreen(nav, it.toRoute()) }
        composable<PlayerRoute> { PlayerScreen(nav, it.toRoute()) }
        composable<SpaceRoute> { SpaceScreen(nav, it.toRoute()) }
        composable<TagRoute> { TagScreen(nav, it.toRoute()) }
        composable<DynamicDetailRoute> { DynamicDetailScreen(nav, it.toRoute()) }
        composable<PgcIndexRoute> { PgcIndexScreen(nav, it.toRoute()) }
        composable<PgcTimelineRoute> { PgcTimelineScreen(nav) }
        composable<HistoryRoute> { HistoryScreen(nav) }
        composable<FavoriteRoute> { FavoriteScreen(nav) }
        composable<ToViewRoute> { ToViewScreen(nav) }
        composable<FollowingSeasonRoute> { FollowingSeasonScreen(nav) }
        composable<FollowingRoute> { FollowingScreen(nav) }
        composable<AccountsRoute> { AccountsScreen(nav) }
        composable<UserLockRoute> { UserLockScreen(nav) }
        composable<LoginRoute> { LoginScreen(nav) }
        composable<SettingsRoute> { SettingsScreen(nav, it.toRoute()) }
        composable<LogsRoute> { LogsScreen(nav) }
        composable<MediaCodecRoute> { MediaCodecScreen(nav) }
        composable<SpeedTestRoute> { SpeedTestScreen(nav) }
    }
}
