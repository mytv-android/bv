package com.github.mytv.bv.ui.designsystem.focus

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import android.view.KeyEvent as AndroidKeyEvent

enum class BvKey { Up, Down, Left, Right, Center, CenterLong, Menu, Back, ChannelUp, ChannelDown, PlayPause, FastForward, Rewind }

fun KeyEvent.toBvKey(): BvKey? {
    val native = nativeKeyEvent
    return when (native.keyCode) {
        AndroidKeyEvent.KEYCODE_DPAD_UP -> BvKey.Up
        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> BvKey.Down
        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> BvKey.Left
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> BvKey.Right
        AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ->
            if (native.isLongPress) BvKey.CenterLong else BvKey.Center

        763 -> BvKey.CenterLong   // 部分盒子的「长按确认」键码
        AndroidKeyEvent.KEYCODE_MENU -> BvKey.Menu
        AndroidKeyEvent.KEYCODE_BACK -> BvKey.Back
        AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_PAGE_UP -> BvKey.ChannelUp
        AndroidKeyEvent.KEYCODE_CHANNEL_DOWN, AndroidKeyEvent.KEYCODE_PAGE_DOWN -> BvKey.ChannelDown
        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, AndroidKeyEvent.KEYCODE_MEDIA_PLAY,
        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE, AndroidKeyEvent.KEYCODE_SPACE -> BvKey.PlayPause

        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> BvKey.FastForward
        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> BvKey.Rewind
        else -> null
    }
}

/** 只处理 KeyDown；handler 返回 true 表示已消费。 */
fun Modifier.onBvKey(handler: (BvKey) -> Boolean): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) false else event.toBvKey()?.let(handler) ?: false
}
