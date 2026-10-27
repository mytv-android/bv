package com.github.mytv.bv.ui.feature.me

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.http.entity.user.MyInfoData
import dev.aaa1115910.biliapi.repositories.HistoryRepository
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.TileRole
import com.github.mytv.bv.ui.navigation.AccountsRoute
import com.github.mytv.bv.ui.navigation.FavoriteRoute
import com.github.mytv.bv.ui.navigation.FollowingRoute
import com.github.mytv.bv.ui.navigation.FollowingSeasonRoute
import com.github.mytv.bv.ui.navigation.HistoryRoute
import com.github.mytv.bv.ui.navigation.ToViewRoute
import com.github.mytv.bv.viewmodel.UserSwitchViewModel
import com.github.mytv.bv.viewmodel.UserViewModel
import com.github.mytv.bv.viewmodel.user.FavoriteViewModel
import com.github.mytv.bv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.github.mytv.bv.repository.UserRepository as BvUserRepository

/** 「我的」的入口（手机彩色卡片、平板左侧列表）。 */
internal enum class MeEntry(
    @param:StringRes val title: Int,
    val icon: ImageVector,
    val role: TileRole,
    val route: Any,
) {
    History(R.string.me_history, Icons.Rounded.History, TileRole.Primary, HistoryRoute),
    Favorite(R.string.me_favorite, Icons.Rounded.Star, TileRole.Tertiary, FavoriteRoute),
    ToView(R.string.me_toview, Icons.Rounded.WatchLater, TileRole.Secondary, ToViewRoute),
    Seasons(R.string.me_seasons, Icons.Rounded.Movie, TileRole.Primary, FollowingSeasonRoute),
    Following(R.string.me_following, Icons.Rounded.Person, TileRole.Primary, FollowingRoute),
    Accounts(R.string.me_accounts, Icons.Rounded.Shield, TileRole.Primary, AccountsRoute);

    companion object {
        /** 四个彩色入口卡（手机 2×2、电视一行 4 个）。 */
        val Tiles = listOf(History, Favorite, ToView, Seasons)
    }
}

/**
 * 三端共享的数据（第 9.8 节「数据」）。ViewModel 在 MeScreen 里创建，按当前账号 uid 区分实例：
 * 切换 / 登录账号后得到新的实例并重新加载。追番、关注列表只有平板右栏用到，在右栏里按同样的 key 创建。
 */
@Stable
internal class MeState(
    val uid: Long,
    val user: UserViewModel,
    val data: MeDataViewModel,
    val favorite: FavoriteViewModel,
    val toView: ToViewViewModel,
    val accounts: UserSwitchViewModel,
) {
    val isLogin: Boolean get() = user.isLogin
    val info: MyInfoData? get() = user.responseData

    /**
     * 历史快照：只在 entries 变化时 toList() 一次，今日数量与「继续观看」共用，
     * 避免每个入口卡各自扫描 SnapshotStateList。
     */
    private val historySnapshotState = derivedStateOf {
        if (isLogin) data.entries.toList() else emptyList()
    }

    /** 继续观看：历史前 20 条（基于同一份历史快照缓存）。 */
    private val continueItemsState = derivedStateOf {
        historySnapshotState.value.take(20)
    }

    private var todayCountSnapshot: List<HistoryEntry>? = null
    private var todayCountDayStart: Long = Long.MIN_VALUE
    private var todayCountValue: Int = 0

    val historySnapshot: List<HistoryEntry> get() = historySnapshotState.value

    val continueItems: List<HistoryEntry> get() = continueItemsState.value

    fun refresh() {
        accounts.updateData()
        if (!isLogin) return
        user.updateUserInfo(forceUpdate = true)
        data.refresh()
        data.loadSeasonTotal()
        if (toView.histories.isEmpty() && !toView.noMore) toView.update()
    }

    fun todayCount(): Int {
        val dayStart = startOfTodaySeconds()
        val snapshot = historySnapshotState.value
        // 跨天后也要重新统计，因此以「今天 0 点时间戳」作为缓存 key，而不是列表长度。
        if (snapshot !== todayCountSnapshot || dayStart != todayCountDayStart) {
            todayCountSnapshot = snapshot
            todayCountDayStart = dayStart
            todayCountValue = snapshot.count { it.viewAt >= dayStart }
        }
        return todayCountValue
    }
}

/** 入口的副标题（未登录或还没取到数据时为空）。[tablet] 时「关注」用「N 位 UP 主」。 */
@Composable
internal fun MeState.subtitle(entry: MeEntry, tablet: Boolean = false): String = when (entry) {
    MeEntry.History -> if (isLogin && (historySnapshot.isNotEmpty() || data.noMore)) {
        stringResource(R.string.me_history_sub, todayCount())
    } else ""

    MeEntry.Favorite -> if (isLogin && !favorite.updatingFolders) {
        stringResource(R.string.me_favorite_sub, favorite.favoriteFolderMetadataList.size)
    } else ""

    MeEntry.ToView -> if (isLogin && (toView.noMore || toView.histories.isNotEmpty())) {
        stringResource(R.string.me_toview_sub, toView.histories.size)
    } else ""

    MeEntry.Seasons -> data.seasonTotal?.takeIf { isLogin }?.let { stringResource(R.string.me_seasons_sub, it) } ?: ""

    MeEntry.Following -> info?.following?.takeIf { isLogin }?.let {
        stringResource(if (tablet) R.string.me_following_sub else R.string.me_following_count, it)
    } ?: ""

    MeEntry.Accounts -> stringResource(R.string.me_accounts_sub, accounts.userDbList.size)
}

/**
 * 我的（第 9.8 节，原型 mePhone / meTablet / meTv）。
 * 数据：UserViewModel（头像、昵称、等级、关注 / 粉丝 / 硬币）、MeDataViewModel（历史，逻辑同 HistoryViewModel）、
 * FavoriteViewModel、ToViewViewModel、FollowingSeasonViewModel、FollowViewModel、UserSwitchViewModel。
 */
@Composable
fun MeScreen(nav: NavController) {
    val userRepository: BvUserRepository = koinInject()
    val historyRepository: HistoryRepository = koinInject()
    val seasonRepository: SeasonRepository = koinInject()
    val uid = userRepository.uid
    val user: UserViewModel = koinViewModel(key = "me_user_$uid")
    val data: MeDataViewModel = viewModel(key = "me_data_$uid") { MeDataViewModel(historyRepository, seasonRepository) }
    val favorite: FavoriteViewModel = koinViewModel(key = "me_favorite_$uid")
    val toView: ToViewViewModel = koinViewModel(key = "me_toview_$uid")
    val accounts: UserSwitchViewModel = koinViewModel()
    val state = remember(uid, user, data, favorite, toView, accounts) {
        MeState(uid, user, data, favorite, toView, accounts)
    }

    // 进入页面（以及从其他页面返回、切换账号）时刷新，同旧电视 UserInfoScreen 的 ON_RESUME 刷新
    LaunchedEffect(state) { state.refresh() }

    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> MePhone(nav, state)
        DeviceLayout.Tablet -> MeTablet(nav, state)
        DeviceLayout.Tv -> MeTv(nav, state)
    }
}
