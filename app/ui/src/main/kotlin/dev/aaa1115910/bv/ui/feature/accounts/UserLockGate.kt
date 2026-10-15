package dev.aaa1115910.bv.ui.feature.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.aaa1115910.bv.repository.UserRepository
import org.koin.compose.koinInject

/**
 * 启动时的「用户锁」（第 8.1 节）：当前用户 lock 不为空时先显示解锁界面（[UnlockUserScreen]），
 * 解锁（或切换到没有锁的账号）后再进入外壳。检查逻辑沿用旧电视版 MainActivity。
 */
@Composable
fun UserLockGate(content: @Composable () -> Unit) {
    val userRepository: UserRepository = koinInject()
    var checking by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val user = runCatching { userRepository.findUserByUid(userRepository.uid) }.getOrNull()
        locked = user?.lock?.isNotBlank() ?: false
        checking = false
    }
    when {
        checking -> Unit
        locked -> UnlockUserScreen(onUnlocked = { locked = false })
        else -> content()
    }
}
