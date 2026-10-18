package dev.aaa1115910.bv.ui.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.ColorTile
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.focus.TvRowPivot
import dev.aaa1115910.bv.ui.feature.timeline.tryRequestFocus
import dev.aaa1115910.bv.ui.navigation.AccountsRoute
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.UserLockRoute

/**
 * 电视「我的」（原型 meTv），导航轨叠在左侧，内容从 x = 96 开始：
 * - 顶部 Row at (96, 27, 48)，spacedBy 20：资料头 + Spacer +「切换账号」「用户锁」（tonal）；
 * - 4 个彩色入口卡 at (96, 118, 48)，4 列 spacedBy 14，聚焦放大 1.05（ColorTile 自带），第一个为初始焦点；
 * - 「继续观看」at (96, 300)：SectionHeader + VideoCard(190, 进度, 无副信息) 横排。
 */
@Composable
internal fun MeTv(nav: NavController, state: MeState) {
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        firstTile.tryRequestFocus()
    }
    val continueItems = state.continueItems

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 96.dp, top = 27.dp, end = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MeHeader(state, onLogin = { nav.navigate(LoginRoute) })
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .focusRestorer()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BvButton(
                    text = stringResource(R.string.me_switch_account),
                    onClick = { nav.navigate(AccountsRoute) },
                    style = BvButtonStyle.Tonal,
                    icon = Icons.Rounded.Person,
                )
                BvButton(
                    text = stringResource(R.string.me_user_lock),
                    onClick = { nav.navigate(UserLockRoute) },
                    style = BvButtonStyle.Tonal,
                    icon = Icons.Rounded.Shield,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 96.dp, top = 118.dp, end = 48.dp)
                .focusRestorer()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MeEntry.Tiles.forEachIndexed { index, entry ->
                ColorTile(
                    title = stringResource(entry.title),
                    subtitle = state.subtitle(entry),
                    icon = entry.icon,
                    role = entry.role,
                    onClick = { nav.navigate(entry.route) },
                    modifier = Modifier
                        .weight(1f)
                        .then(if (index == 0) Modifier.focusRequester(firstTile) else Modifier),
                )
            }
        }

        if (continueItems.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 300.dp)
            ) {
                SectionHeader(
                    title = stringResource(R.string.me_continue),
                    modifier = Modifier.padding(start = 96.dp, end = 48.dp),
                )
                TvRowPivot {
                    LazyRow(
                        state = rememberLazyListState(),
                        // 视口从导航轨右边缘（收起 76dp）开始：往右翻时卡片不会滑到侧栏下面
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 76.dp)
                            .focusRestorer()
                            .focusGroup(),
                        contentPadding = PaddingValues(start = 24.dp, end = 48.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(continueItems) { entry ->
                            VideoCard(
                                data = entry.card,
                                onClick = { nav.openHistory(entry) },
                                width = 190.dp,
                                progress = entry.progress,
                                showMeta = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
