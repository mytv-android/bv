@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.design

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import android.view.KeyEvent as AndroidKeyEvent

// 照抄 app:ui designsystem/focus/BvKey.kt（第 6.8 节）。player:ui 不能依赖 app:ui，所以在这里放一份；
// 可见性改为 internal，避免与 app:ui 的同名公开 API 混淆。

internal enum class BvKey { Up, Down, Left, Right, Center, CenterLong, Menu, Back, ChannelUp, ChannelDown, PlayPause, FastForward, Rewind }

internal fun KeyEvent.toBvKey(): BvKey? {
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
internal fun Modifier.onBvKey(handler: (BvKey) -> Boolean): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) false else event.toBvKey()?.let(handler) ?: false
}

/**
 * player:ui 补充：只处理 KeyUp。旧电视播放器的确认 / 上 / 下 / 菜单 / 返回键都是「抬起时触发」
 * （按下时先吞掉，用来区分确认键短按与长按），用它照搬这一行为。映射同样走 [toBvKey]。
 */
internal fun Modifier.onBvKeyUp(handler: (BvKey) -> Boolean): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyUp) false else event.toBvKey()?.let(handler) ?: false
}
