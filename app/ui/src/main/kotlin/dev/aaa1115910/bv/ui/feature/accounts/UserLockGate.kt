package dev.aaa1115910.bv.ui.feature.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.ui.navigation.PlaceholderPage
import org.koin.compose.koinInject

/**
 * 启动时的「用户锁」：当前用户 lock 不为空时先解锁，再进入外壳（沿用旧电视版 MainActivity 的逻辑）。
 * TODO(redesign 步骤 8)：解锁界面（电视居中卡片，方向键序列；手机 4 位数字）。
 */
@Composable
fun UserLockGate(content: @Composable () -> Unit) {
    val userRepository: UserRepository = koinInject()
    var checking by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val user = userRepository.findUserByUid(userRepository.uid)
        locked = user?.lock?.isNotBlank() ?: false
        checking = false
    }
    when {
        checking -> Unit
        locked -> PlaceholderPage("UnlockUser")
        else -> content()
    }
}
