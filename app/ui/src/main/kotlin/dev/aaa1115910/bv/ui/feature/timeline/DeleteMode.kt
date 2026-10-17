package dev.aaa1115910.bv.ui.feature.timeline

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.theme.BvType

/*
 * 历史 / 稍后再看 / 收藏 / 追番共用的删除模式：
 * - 电视：菜单键或长按确认键进入删除模式；删除模式内菜单键 / 长按确认键在支持清空的列表里触发清空（二次确认），
 *   否则菜单键退出、长按确认键直接删除当前选中项；返回键退出（各页面 BackHandler）。
 * - 触屏：长按卡片进入删除模式，删除模式下卡片上显示删除按钮。
 */

/**
 * 删除模式按键。长按确认键时先收到一次普通按下、再收到长按事件，这里在长按后把随后的 KeyUp 也吞掉，
 * 避免卡片把这次长按当成一次点击（打开视频 / 打开确认框）。
 */
@Composable
internal fun Modifier.deleteModeKeys(
    deleteMode: Boolean,
    onEnter: () -> Unit,
    onExit: () -> Unit,
    onClear: (() -> Unit)? = null,
    onDirectDelete: (() -> Unit)? = null,
): Modifier {
    val guard = remember { DeleteKeyGuard() }
    return onPreviewKeyEvent { event ->
        guard.handle(event, deleteMode, onEnter, onExit, onClear, onDirectDelete)
    }
}

@Stable
private class DeleteKeyGuard {
    private var suppressCenterUp = false
    private var suppressMenuUp = false
    /** 长按确认键会持续发来重复的 KeyDown，处理一次后忽略到抬手为止 */
    private var ignoreCenterLongUntilUp = false

    fun handle(
        event: KeyEvent,
        deleteMode: Boolean,
        onEnter: () -> Unit,
        onExit: () -> Unit,
        onClear: (() -> Unit)?,
        onDirectDelete: (() -> Unit)?,
    ): Boolean {
        val native = event.nativeKeyEvent
        val code = native.keyCode
        val centerKey = code == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
            code == AndroidKeyEvent.KEYCODE_ENTER ||
            code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
        val longPressCode = code == LONG_PRESS_KEY_CODE
        val menuKey = code == AndroidKeyEvent.KEYCODE_MENU

        if (event.type == KeyEventType.KeyUp) {
            if (centerKey || longPressCode) ignoreCenterLongUntilUp = false
            return when {
                (centerKey || longPressCode) && suppressCenterUp -> {
                    suppressCenterUp = false
                    true
                }

                menuKey && suppressMenuUp -> {
                    suppressMenuUp = false
                    true
                }

                else -> false
            }
        }
        if (event.type != KeyEventType.KeyDown) return false

        // 新的一次普通按下（长按序列的第一下）先把上一次遗留的状态清掉
        if (centerKey && !native.isLongPress) ignoreCenterLongUntilUp = false
        val centerLong = (centerKey && native.isLongPress) || longPressCode
        if (centerLong && ignoreCenterLongUntilUp) return true
        if (!deleteMode) {
            return when {
                menuKey -> {
                    suppressMenuUp = true
                    onEnter()
                    true
                }

                centerLong -> {
                    ignoreCenterLongUntilUp = true
                    suppressCenterUp = true
                    onEnter()
                    true
                }

                else -> false
            }
        }

        return when {
            menuKey -> {
                suppressMenuUp = true
                if (onClear != null) onClear() else onExit()
                true
            }

            centerLong -> {
                ignoreCenterLongUntilUp = true
                when {
                    // 打开确认框时由对话框自己吞掉这次长按的 KeyUp
                    onClear != null -> {
                        onClear()
                        true
                    }

                    onDirectDelete != null -> {
                        suppressCenterUp = true
                        onDirectDelete()
                        true
                    }

                    else -> false
                }
            }

            else -> false
        }
    }

    private companion object {
        /** 部分盒子的「长按确认」键码 */
        const val LONG_PRESS_KEY_CODE = 763
    }
}

/** 删除模式提示：电视上始终提示进入方式；进入删除模式后三端都显示操作说明。 */
@Composable
internal fun DeleteModeHint(
    deleteMode: Boolean,
    clearSupported: Boolean,
    dpad: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = when {
        deleteMode -> stringResource(
            if (clearSupported) R.string.ds_delete_mode_clear_hint
            else R.string.ds_delete_mode_delete_hint
        )

        dpad -> stringResource(R.string.ds_delete_mode_enter_hint)
        else -> return
    }
    Text(
        text = text,
        modifier = modifier,
        style = BvType.Meta,
        color = if (deleteMode) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 吞掉打开对话框那一次长按确认键的 KeyUp，避免焦点落在按钮上时误触发点击。
 * 只在打开后的很短一段时间内生效：点按打开时 KeyUp 已经过去，不会把用户随后的确认操作吞掉。
 */
private class InitialKeyUpGuard {
    private var armedUntil = 0L

    fun arm() {
        armedUntil = android.os.SystemClock.uptimeMillis() + 400L
    }

    fun consume(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyUp) return false
        if (android.os.SystemClock.uptimeMillis() > armedUntil) return false
        val code = event.nativeKeyEvent.keyCode
        if (code != AndroidKeyEvent.KEYCODE_DPAD_CENTER &&
            code != AndroidKeyEvent.KEYCODE_ENTER &&
            code != AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
        ) return false
        armedUntil = 0L
        return true
    }
}

/** 删除确认对话框（单条删除 / 清空共用）。电视上初始焦点放在「取消」上，避免误确认。 */
@Composable
internal fun DeleteConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.ds_delete_confirm),
    dismissText: String = stringResource(R.string.ds_cancel),
) {
    val dismissRequester = remember { FocusRequester() }
    val guard = remember { InitialKeyUpGuard() }
    LaunchedEffect(Unit) {
        guard.arm()
        withFrameNanos { }
        dismissRequester.tryRequestFocus()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.onPreviewKeyEvent { guard.consume(it) },
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .focusRequester(dismissRequester)
                    .onPreviewKeyEvent { guard.consume(it) },
            ) { Text(dismissText) }
        },
    )
}
