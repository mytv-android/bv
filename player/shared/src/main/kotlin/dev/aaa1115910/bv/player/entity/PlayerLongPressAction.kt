package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.player.shared.R

/** 电视端播放器中长按确认键的行为（照搬 fantasy：0 = 打开菜单，1 = 加速播放）。 */
enum class PlayerLongPressAction(private val strRes: Int) {
    OpenMenu(R.string.player_long_press_action_open_menu),
    SpeedUp(R.string.player_long_press_action_speed_up);

    fun getDisplayName(context: Context) = context.getString(strRes)
}
