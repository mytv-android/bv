package com.github.mytv.bv.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.icon.BvIcons
import kotlin.reflect.KClass

/** 六个一级入口（顺序固定）：首页 · 刷刷 · 影视 · 直播 · 动态 · 我的。 */
enum class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    @param:StringRes val label: Int,
    val icon: () -> ImageVector,
    val iconSelected: () -> ImageVector,
) {
    Home(HomeRoute, HomeRoute::class, R.string.nav_home, { Icons.Outlined.Home }, { Icons.Filled.Home }),
    Shorts(ShortsRoute(), ShortsRoute::class, R.string.nav_shorts, { BvIcons.Shorts }, { BvIcons.ShortsFilled }),
    Pgc(PgcRoute, PgcRoute::class, R.string.nav_pgc, { Icons.Outlined.Movie }, { Icons.Filled.Movie }),
    Live(LiveRoute, LiveRoute::class, R.string.nav_live, { Icons.Outlined.LiveTv }, { Icons.Filled.LiveTv }),
    Dynamic(DynamicRoute, DynamicRoute::class, R.string.nav_dynamic, { Icons.Outlined.Subscriptions }, { Icons.Filled.Subscriptions }),
    Me(MeRoute, MeRoute::class, R.string.nav_me, { Icons.Outlined.Person }, { Icons.Filled.Person });

    companion object {
        fun of(destination: NavDestination?): TopLevel? =
            destination?.let { dest -> entries.firstOrNull { dest.hasRoute(it.routeClass) } }
    }
}
