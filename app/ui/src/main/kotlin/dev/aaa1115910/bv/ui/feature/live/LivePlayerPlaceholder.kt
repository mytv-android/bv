package dev.aaa1115910.bv.ui.feature.live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.timeline.SubPageFrame
import dev.aaa1115910.bv.ui.navigation.LivePlayerRoute

/**
 * 直播间占位页：BvLivePlayerScreen（播放器 + 互动区）由下一个任务实现，届时整体替换本页面。
 * 路由 [LivePlayerRoute] 已按最终形态注册（roomId 参数）。
 */
@Composable
fun LivePlayerPlaceholder(nav: NavController, route: LivePlayerRoute) {
    SubPageFrame(title = stringResource(R.string.live_room_title, route.roomId), onBack = { nav.popBackStack() }) { _ ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.live_player_placeholder),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
