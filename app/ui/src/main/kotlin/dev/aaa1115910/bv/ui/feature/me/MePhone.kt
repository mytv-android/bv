package dev.aaa1115910.bv.ui.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvListItem
import dev.aaa1115910.bv.ui.designsystem.component.ColorTile
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.feature.home.fullBleed
import dev.aaa1115910.bv.ui.navigation.AccountsRoute
import dev.aaa1115910.bv.ui.navigation.FollowingRoute
import dev.aaa1115910.bv.ui.navigation.HistoryRoute
import dev.aaa1115910.bv.ui.navigation.LocalBottomBarPadding
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.SettingsRoute

/**
 * 手机「我的」（原型 mePhone）：Column(padding(top = statusBar + 16, 水平 20), spacedBy 18)：
 * 资料头 → 2×2 彩色入口卡（spacedBy 10）→「继续观看」+「全部」→ 横向 VideoCard(150, 进度, 无副信息)
 * → 列表卡（圆角 24、surfaceContainerLow、padding 6）：关注的 UP 主 / 账号与用户锁 / 设置。
 */
@Composable
internal fun MePhone(nav: NavController, state: MeState) {
    val bottom = LocalBottomBarPadding.current + 16.dp
    val continueItems = state.continueItems
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        MeHeader(state, onLogin = { nav.navigate(LoginRoute) })

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MeEntry.Tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { entry ->
                        ColorTile(
                            title = stringResource(entry.title),
                            subtitle = state.subtitle(entry),
                            icon = entry.icon,
                            role = entry.role,
                            onClick = { nav.navigate(entry.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (continueItems.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.me_continue),
                action = stringResource(R.string.me_all),
                onAction = { nav.navigate(HistoryRoute) },
            )
            LazyRow(
                modifier = Modifier.fullBleed(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = continueItems,
                    key = { it.card.avid },
                    contentType = { "video" },
                ) { entry ->
                    VideoCard(
                        data = entry.card,
                        onClick = { nav.openHistory(entry) },
                        width = 150.dp,
                        progress = entry.progress,
                        showMeta = false,
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(6.dp)
        ) {
            val iconContainer = MaterialTheme.colorScheme.surfaceContainerHighest
            BvListItem(
                title = stringResource(R.string.me_following_ups),
                subtitle = state.subtitle(MeEntry.Following).ifEmpty { null },
                icon = Icons.Rounded.Person,
                iconContainerColor = iconContainer,
                onClick = { nav.navigate(FollowingRoute) },
            )
            BvListItem(
                title = stringResource(R.string.me_accounts),
                subtitle = state.subtitle(MeEntry.Accounts),
                icon = Icons.Rounded.Shield,
                iconContainerColor = iconContainer,
                onClick = { nav.navigate(AccountsRoute) },
            )
            BvListItem(
                title = stringResource(R.string.me_settings),
                subtitle = stringResource(R.string.me_settings_sub),
                icon = Icons.Rounded.Settings,
                iconContainerColor = iconContainer,
                onClick = { nav.navigate(SettingsRoute()) },
            )
        }
    }
}
