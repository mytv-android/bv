package com.github.mytv.bv.ui.feature.player

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.player.entity.VideoListInteractiveNode
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvListItem
import com.github.mytv.bv.ui.designsystem.theme.BvType
import kotlinx.coroutines.delay

/**
 * 互动视频选项对话框（旧电视 InteractiveOptionDialog 的三端统一版）：
 * 节点播完弹出，触屏直接点选，电视端上下键在选项间移动、确认键选择；
 * 关闭只是收起弹窗（播完处于暂停状态），「退出播放」才是离开播放器。
 */
@Composable
internal fun InteractiveOptionDialog(
    options: List<VideoListInteractiveNode>,
    currentCid: Long,
    onSelectOption: (VideoListInteractiveNode) -> Unit,
    onDismiss: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstRequester = remember(options.size) { FocusRequester() }
    // 等对话框窗口完成组合再抢焦点（旧电视版同款 120ms）
    LaunchedEffect(options.size) {
        delay(120)
        runCatching { firstRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier.widthIn(min = 280.dp, max = 520.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.player_screen_interactive_title), style = BvType.PanelTitle)
                Text(
                    stringResource(R.string.player_screen_interactive_description),
                    style = BvType.Small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                        .focusRestorer(firstRequester)
                        .focusGroup(),
                ) {
                    options.forEachIndexed { index, option ->
                        val label = option.partTitle.ifBlank {
                            stringResource(R.string.player_screen_interactive_option_fallback, index + 1)
                        }
                        val requesterModifier = if (index == 0) {
                            Modifier.focusRequester(firstRequester)
                        } else {
                            Modifier
                        }
                        BvListItem(
                            modifier = requesterModifier.fillMaxWidth(),
                            title = label,
                            selected = option.isCurrent || option.cid == currentCid,
                            icon = if (option.isCurrent || option.cid == currentCid) Icons.Filled.PlayArrow else null,
                            iconContainerColor = MaterialTheme.colorScheme.primary,
                            iconContentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { onSelectOption(option) },
                        )
                    }
                }
                BvButton(
                    text = stringResource(R.string.player_screen_interactive_exit),
                    onClick = onExit,
                    modifier = Modifier.align(Alignment.End),
                    style = BvButtonStyle.Tonal,
                )
            }
        }
    }
}
