@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import dev.aaa1115910.bv.player.ui.R
import dev.aaa1115910.bv.player.ui.design.BvKey
import dev.aaa1115910.bv.player.ui.design.PlayerType
import dev.aaa1115910.bv.player.ui.design.onBvKey
import dev.aaa1115910.bv.player.ui.design.onBvKeyUp
import dev.aaa1115910.bv.util.requestFocus

/**
 * 遥控器操作提示（由旧 app/tv component/RemoteControlPanelDemo 改写，换成本模块的样式）。
 * 铺满父容器，确认键（或返回键 / 点击）关闭。是否只显示一次由调用方决定。
 *
 * 也可以通过 [dev.aaa1115910.bv.player.ui.BvPlayerScreen] 的 `showRemoteControlHint` 参数让播放器自己显示，
 * 这样关闭后焦点会自动回到播放器。
 */
@Composable
fun RemoteControlHintOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .focusRequester(focusRequester)
            // 按下时吞掉所有按键；确认 / 返回在抬起时关闭，避免抬起事件落到下面的播放器上
            .onBvKey { true }
            .onBvKeyUp { key ->
                if (key == BvKey.Center || key == BvKey.Back) onDismiss()
                true
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RemoteControlPanelInfo()
            Text(
                text = stringResource(R.string.player_ui_remote_tip_bottom),
                style = PlayerType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 遥控器示意图 + 各键说明，几何尺寸照搬旧版。 */
@Composable
private fun RemoteControlPanelInfo() {
    ConstraintLayout {
        val (panelBorder, dPadBorder, centerBorder, backBorder) = createRefs()
        val borderWidth = 3.dp
        val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
        val keyColor = MaterialTheme.colorScheme.onSurface
        val lineColor = MaterialTheme.colorScheme.outline
        val tipTextStyle = PlayerType.HeroPhone.copy(color = MaterialTheme.colorScheme.onSurface)

        Box(
            modifier = Modifier
                .constrainAs(panelBorder) {
                    centerTo(parent)
                }
                .size(200.dp, 400.dp)
                .border(borderWidth, outlineColor, RoundedCornerShape(100.dp))
        )
        Box(
            modifier = Modifier
                .constrainAs(dPadBorder) {
                    top.linkTo(panelBorder.top, 8.dp)
                    start.linkTo(panelBorder.start)
                    end.linkTo(panelBorder.end)
                }
                .size(180.dp)
                .border(borderWidth, keyColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .constrainAs(centerBorder) {
                    centerTo(dPadBorder)
                }
                .size(70.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Box(
            modifier = Modifier
                .constrainAs(backBorder) {
                    top.linkTo(dPadBorder.bottom, 16.dp)
                    start.linkTo(panelBorder.start, 16.dp)
                }
                .size(70.dp)
                .border(borderWidth, keyColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = keyColor
            )
        }

        val (tipLineBack, tipBack) = createRefs()
        Spacer(
            modifier = Modifier
                .constrainAs(tipLineBack) {
                    end.linkTo(backBorder.start)
                    top.linkTo(backBorder.top)
                    bottom.linkTo(backBorder.bottom)
                }
                .width(80.dp)
                .height(borderWidth / 2)
                .background(lineColor)
        )
        Text(
            modifier = Modifier
                .constrainAs(tipBack) {
                    top.linkTo(tipLineBack.top)
                    bottom.linkTo(tipLineBack.bottom)
                    end.linkTo(tipLineBack.start, 8.dp)
                },
            text = stringResource(R.string.player_ui_remote_tip_back),
            style = tipTextStyle
        )

        val (tipLineCenter, tipCenter) = createRefs()
        Spacer(
            modifier = Modifier
                .constrainAs(tipLineCenter) {
                    start.linkTo(centerBorder.end)
                    top.linkTo(centerBorder.top)
                    bottom.linkTo(centerBorder.bottom)
                }
                .width(125.dp)
                .height(borderWidth / 2)
                .background(lineColor)
        )
        Text(
            modifier = Modifier
                .constrainAs(tipCenter) {
                    top.linkTo(tipLineCenter.top)
                    bottom.linkTo(tipLineCenter.bottom)
                    start.linkTo(tipLineCenter.end, 8.dp)
                },
            text = stringResource(R.string.player_ui_remote_tip_center),
            style = tipTextStyle
        )

        val (tipLineUp, tipUp) = createRefs()
        Spacer(
            modifier = Modifier
                .constrainAs(tipLineUp) {
                    start.linkTo(dPadBorder.end, (-80).dp)
                    top.linkTo(dPadBorder.top, 20.dp)
                }
                .width(150.dp)
                .height(borderWidth / 2)
                .background(lineColor)
        )
        Text(
            modifier = Modifier
                .constrainAs(tipUp) {
                    top.linkTo(tipLineUp.top)
                    bottom.linkTo(tipLineUp.bottom)
                    start.linkTo(tipLineUp.end, 8.dp)
                },
            text = stringResource(R.string.player_ui_remote_tip_up),
            style = tipTextStyle
        )

        val (tipLineDown, tipDown) = createRefs()
        Spacer(
            modifier = Modifier
                .constrainAs(tipLineDown) {
                    start.linkTo(dPadBorder.end, (-80).dp)
                    bottom.linkTo(dPadBorder.bottom, 20.dp)
                }
                .width(150.dp)
                .height(borderWidth / 2)
                .background(lineColor)
        )
        Text(
            modifier = Modifier
                .constrainAs(tipDown) {
                    top.linkTo(tipLineDown.top)
                    bottom.linkTo(tipLineDown.bottom)
                    start.linkTo(tipLineDown.end, 8.dp)
                },
            text = stringResource(R.string.player_ui_remote_tip_down),
            style = tipTextStyle
        )

        val (tipLineLR1, tipLineLR2, tipLineLR3, tipLR) = createRefs()
        Spacer(
            modifier = Modifier
                .constrainAs(tipLineLR1) {
                    end.linkTo(dPadBorder.end, 20.dp)
                    top.linkTo(dPadBorder.top, 60.dp)
                }
                .rotate(45f)
                .width(40.dp)
                .height(borderWidth / 2)
                .background(lineColor)
        )
        Spacer(
            modifier = Modifier
                .constrainAs(tipLineLR3) {
                    end.linkTo(tipLineLR1.end, 120.dp)
                    top.linkTo(tipLineLR1.top)
                }
                .rotate(45f)
                .width(40.dp)
                .height(borderWidth / 2)
                .background(lineColor)
        )
        Spacer(
            modifier = Modifier
                .constrainAs(tipLineLR2) {
                    end.linkTo(tipLineLR1.start, (-7).dp)
                    top.linkTo(tipLineLR1.top, (-14).dp)
                }
                .width(200.dp)
                .height(borderWidth / 2)
                .background(lineColor)
        )
        Text(
            modifier = Modifier
                .constrainAs(tipLR) {
                    top.linkTo(tipLineLR2.top)
                    bottom.linkTo(tipLineLR2.bottom)
                    end.linkTo(tipLineLR2.start, 8.dp)
                },
            text = stringResource(R.string.player_ui_remote_tip_lr),
            style = tipTextStyle
        )
    }
}
