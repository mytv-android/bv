@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.touch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.player.ui.R
import dev.aaa1115910.bv.player.ui.design.PlayerType
import dev.aaa1115910.bv.util.formatHourMinSec
import kotlinx.coroutines.delay

// 触摸手势的提示，逻辑照搬旧 player/mobile controller/Tips.kt，样式改为圆角 16 的半透明黑底。

private val TouchTipShape = RoundedCornerShape(16.dp)
private val TouchTipColor = Color.Black.copy(alpha = 0.6f)

@Composable
private fun CenterTip(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(TouchTipShape)
                .background(TouchTipColor)
        ) {
            content()
        }
    }
}

@Composable
internal fun SeekMoveTip(
    show: Boolean,
    startTime: Long,
    move: Long,
    totalTime: Long,
    modifier: Modifier = Modifier,
) {
    if (!show) return
    val target = (startTime + move).coerceIn(0L, totalTime.coerceAtLeast(0L))
    CenterTip(modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(
                R.string.player_ui_time_progress,
                target.formatHourMinSec(),
                totalTime.formatHourMinSec()
            ),
            style = PlayerType.BodyBold,
            color = Color.White
        )
    }
}

@Composable
internal fun QuickDoubleSpeedPlaybackTip(
    show: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!show) return
    CenterTip(modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.player_ui_double_speed_tip),
            style = PlayerType.BodyBold,
            color = Color.White
        )
    }
}

@Composable
private fun LevelTip(
    show: Boolean,
    progress: Float,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val displayProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = label
    )
    var showTip by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (!show) {
            delay(500)
            showTip = false
        } else {
            showTip = true
        }
    }

    if (!showTip) return
    CenterTip(modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            LinearProgressIndicator(
                modifier = Modifier.width(100.dp),
                progress = { displayProgress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
internal fun BrightnessTip(
    show: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LevelTip(
        modifier = modifier,
        show = show,
        progress = progress,
        icon = when {
            progress < 1 / 3f -> Icons.Rounded.BrightnessLow
            progress < 2 / 3f -> Icons.Rounded.BrightnessMedium
            else -> Icons.Rounded.BrightnessHigh
        },
        label = "BrightnessTipProgress",
    )
}

@Composable
internal fun VolumeTip(
    show: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LevelTip(
        modifier = modifier,
        show = show,
        progress = progress,
        icon = when {
            progress == 0f -> Icons.AutoMirrored.Rounded.VolumeOff
            progress < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
            else -> Icons.AutoMirrored.Rounded.VolumeUp
        },
        label = "VolumeTipProgress",
    )
}

/** 「上次看到 xx，点击跳转」：左下角，点击跳回上次进度。 */
@Composable
internal fun BackToHistoryTip(
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.player_ui_back_to_history_touch, time),
            style = PlayerType.BodyBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
