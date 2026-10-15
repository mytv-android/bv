@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.player.ui.R
import dev.aaa1115910.bv.player.ui.design.PlayerType

/** 暂停中且控制层隐藏时，右下角的暂停图标（照搬旧电视版 PauseIcon）。 */
@Composable
internal fun PauseIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Icon(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .size(50.dp),
            imageVector = Icons.Rounded.Pause,
            contentDescription = null,
            tint = Color.White
        )
    }
}

/** 「上次看到 xx 点击确认键跳转」（照搬旧电视版 SkipTips / BackToHistoryTip）。 */
@Composable
internal fun TvBackToHistoryTip(
    show: Boolean,
    time: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = show,
        enter = slideInHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { -it } +
                fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = slideOutHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()) { -it } +
                fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.player_ui_back_to_history_tv, time),
                style = PlayerType.BodyBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
