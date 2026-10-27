package com.github.mytv.bv.ui.feature.accounts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.db.UserDB
import com.github.mytv.bv.ui.designsystem.adaptive.BvInputMode
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvIconButton
import com.github.mytv.bv.ui.designsystem.component.BvIconButtonStyle
import com.github.mytv.bv.ui.designsystem.component.BvListItem
import com.github.mytv.bv.ui.designsystem.component.FullPageLoading
import com.github.mytv.bv.ui.designsystem.focus.BvKey
import com.github.mytv.bv.ui.designsystem.focus.TvColumnPivot
import com.github.mytv.bv.ui.designsystem.focus.onBvKey
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.login.LoginDialogFrame
import com.github.mytv.bv.ui.feature.timeline.SubPageFrame
import com.github.mytv.bv.ui.feature.timeline.tryRequestFocus
import com.github.mytv.bv.ui.navigation.LoginRoute
import com.github.mytv.bv.ui.navigation.UserLockRoute
import com.github.mytv.bv.viewmodel.UserSwitchViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.github.mytv.bv.repository.UserRepository as BvUserRepository

private val accountsLogger = KotlinLogging.logger("BvAccounts")

/**
 * 账号管理（第 9.11 节模板，数据 UserSwitchViewModel；逻辑照抄旧电视 UserSwitchScreen）：
 * 已保存账号列表（点击切换，有用户锁的账号先输入密码；右侧删除）+「添加账号」→ LoginRoute +「用户锁」→ UserLockRoute。
 * 切换成功后返回上一页。电视：可聚焦列表，初始焦点在当前账号。
 */
@Composable
fun AccountsScreen(nav: NavController) {
    val vm: UserSwitchViewModel = koinViewModel()
    LaunchedEffect(vm) { vm.updateData() }
    SubPageFrame(title = stringResource(R.string.accounts_list_title), onBack = { nav.popBackStack() }) { spec ->
        AccountsContent(
            vm = vm,
            onAdd = { nav.navigate(LoginRoute) },
            onUserLock = { nav.navigate(UserLockRoute) },
            onSwitched = { nav.popBackStack() },
            contentPadding = PaddingValues(
                start = if (spec.tv) spec.horizontal else 8.dp,
                end = if (spec.tv) spec.horizontal else 8.dp,
                top = spec.top,
                bottom = spec.bottom,
            ),
            modifier = Modifier.weight(1f),
            tv = spec.tv,
        )
    }
}

/**
 * 账号列表（「我的」平板右栏「账号与用户锁」也用）。[onSwitched] 在切换成功后调用。
 */
@Composable
internal fun AccountsContent(
    vm: UserSwitchViewModel,
    onAdd: () -> Unit,
    onUserLock: () -> Unit,
    onSwitched: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    tv: Boolean = LocalDeviceProfile.current.isTv,
) {
    val userRepository: BvUserRepository = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val users = vm.userDbList
    val currentUid = userRepository.uid
    var busy by remember { mutableStateOf(false) }
    var unlockFor by remember { mutableStateOf<UserDB?>(null) }
    var deleteFor by remember { mutableStateOf<UserDB?>(null) }
    val initialRequester = remember { FocusRequester() }
    val addRequester = remember { FocusRequester() }
    var entryPending by remember { mutableStateOf(true) }

    LaunchedEffect(vm.loading) {
        if (!tv || !entryPending || vm.loading) return@LaunchedEffect
        withFrameNanos { }
        if (!initialRequester.tryRequestFocus()) addRequester.tryRequestFocus()
        entryPending = false
    }

    // 切换在主线程执行：UserRepository.setUser 会写 Compose 状态，数据库 / 网络调用都是挂起函数
    val doSwitch: (UserDB) -> Unit = { user ->
        if (!busy) {
            busy = true
            scope.launch {
                val ok = runCatching { vm.switchUser(user) }
                    .onFailure { accountsLogger.warn(it) { "switch user failed" } }
                    .isSuccess
                runCatching { vm.updateUserDbList() }
                busy = false
                if (ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.accounts_list_switched, user.username),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSwitched()
                }
            }
        }
    }
    val onSwitch: (UserDB) -> Unit = { user ->
        when {
            user.uid == currentUid -> Unit
            user.lock.isNotBlank() -> unlockFor = user
            else -> doSwitch(user)
        }
    }
    // 删除当前账号：同旧版 deleteUser（切到剩下的第一个账号，没有就退出登录）；删除其他账号只删记录，不切换
    val doDelete: (UserDB) -> Unit = { user ->
        if (!busy) {
            busy = true
            scope.launch {
                runCatching {
                    if (user.uid == userRepository.uid) {
                        vm.deleteUser(user)
                    } else {
                        BVApp.getAppDatabase().userDao().delete(user)
                        vm.updateUserDbList()
                    }
                }.onFailure { accountsLogger.warn(it) { "delete user failed" } }
                busy = false
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        if (vm.loading && users.isEmpty()) {
            FullPageLoading()
        } else {
            val initialUid = users.firstOrNull { it.uid == currentUid }?.uid ?: users.firstOrNull()?.uid
            TvColumnPivot {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (tv) Modifier.focusRestorer().focusGroup() else Modifier),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(if (tv) 10.dp else 4.dp),
                ) {
                    if (users.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            Text(
                                stringResource(R.string.accounts_list_empty),
                                modifier = Modifier.fillMaxWidth(),
                                style = BvType.Body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    itemsIndexed(
                        items = users,
                        key = { _, user -> user.uid },
                        contentType = { _, _ -> "account" },
                    ) { _, user ->
                        AccountRow(
                            user = user,
                            current = user.uid == currentUid,
                            onClick = { onSwitch(user) },
                            onDelete = { deleteFor = user },
                            modifier = if (user.uid == initialUid) Modifier.focusRequester(initialRequester) else Modifier,
                        )
                    }
                    item(key = "add", contentType = "action") {
                        BvListItem(
                            title = stringResource(R.string.accounts_list_add),
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .focusRequester(addRequester),
                            subtitle = stringResource(R.string.accounts_list_add_sub),
                            icon = Icons.Rounded.PersonAdd,
                            onClick = onAdd,
                        )
                    }
                    item(key = "lock", contentType = "action") {
                        BvListItem(
                            title = stringResource(R.string.accounts_list_lock),
                            modifier = Modifier.widthIn(max = 720.dp),
                            subtitle = stringResource(R.string.accounts_list_lock_sub),
                            icon = Icons.Rounded.Lock,
                            onClick = onUserLock,
                        )
                    }
                }
            }
        }
        if (busy) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        }
    }

    unlockFor?.let { user ->
        AccountUnlockDialog(
            user = user,
            onDismiss = { unlockFor = null },
            onUnlocked = {
                unlockFor = null
                doSwitch(user)
            },
        )
    }
    deleteFor?.let { user ->
        LoginDialogFrame(onDismiss = { deleteFor = null }) {
            Text(stringResource(R.string.accounts_list_delete), style = BvType.PanelTitle)
            Text(
                stringResource(R.string.accounts_list_delete_message, user.username),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier
                    .align(Alignment.End)
                    .focusRestorer()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BvButton(
                    text = stringResource(R.string.accounts_list_cancel),
                    onClick = { deleteFor = null },
                    style = BvButtonStyle.Outlined,
                )
                BvButton(
                    text = stringResource(R.string.accounts_list_confirm_delete),
                    onClick = {
                        deleteFor = null
                        doDelete(user)
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

/** 一个账号：BvListItem（头像 40 + 名字 + 当前 / 已设锁）+ 右侧删除按钮（与列表项并列，方向键可左右移动）。 */
@Composable
private fun AccountRow(
    user: UserDB,
    current: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = user.lock.isNotBlank()
    val subtitle = when {
        current && locked -> stringResource(R.string.accounts_list_current_locked)
        current -> stringResource(R.string.accounts_list_current)
        locked -> stringResource(R.string.accounts_list_locked)
        else -> null
    }
    Row(
        Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvListItem(
            title = user.username,
            modifier = modifier.weight(1f),
            subtitle = subtitle,
            selected = current,
            onClick = onClick,
            leading = { BvAvatar(url = user.avatar.ifBlank { null }, size = 40.dp, fallbackText = user.username) },
        )
        BvIconButton(
            icon = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.accounts_list_delete),
            onClick = onDelete,
        )
    }
}

/** 方向键序列密码的字符（同旧电视用户锁：u d l r）。 */
private fun BvKey.toLockChar(): Char? = when (this) {
    BvKey.Up -> 'u'
    BvKey.Down -> 'd'
    BvKey.Left -> 'l'
    BvKey.Right -> 'r'
    else -> null
}

/**
 * 切换到有用户锁的账号前输入密码（旧电视 UnlockSwitchUserContent 的逻辑：lock == 输入）。
 * 数字密码（触屏设置的 4 位）用数字键盘、输满自动校验；方向序列密码：电视上直接按方向键、确认键校验，
 * 触屏上显示方向键盘。
 */
@Composable
private fun AccountUnlockDialog(user: UserDB, onDismiss: () -> Unit, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val dpad = LocalDeviceProfile.current.input == BvInputMode.DPad
    val lock = user.lock
    val numeric = lock.isNotEmpty() && lock.all { it.isDigit() }
    var input by remember { mutableStateOf("") }
    val captureRequester = remember { FocusRequester() }
    val padRequester = remember { FocusRequester() }

    val submit: () -> Unit = {
        if (input == lock) {
            onUnlocked()
        } else {
            Toast.makeText(context, R.string.accounts_unlock_wrong_password, Toast.LENGTH_SHORT).show()
            input = ""
        }
    }
    LaunchedEffect(input) {
        if (numeric && input.length >= lock.length) submit()
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!numeric && dpad) captureRequester.tryRequestFocus() else padRequester.tryRequestFocus()
    }

    LoginDialogFrame(onDismiss = onDismiss) {
        Text(stringResource(R.string.accounts_list_unlock_title, user.username), style = BvType.PanelTitle)
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(
                    if (!numeric && dpad) {
                        Modifier
                            .focusRequester(captureRequester)
                            .onBvKey { key ->
                                val char = key.toLockChar()
                                when {
                                    char != null -> {
                                        input += char
                                        true
                                    }

                                    key == BvKey.Center -> {
                                        submit()
                                        true
                                    }

                                    else -> false
                                }
                            }
                            .focusable()
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val count = if (numeric) lock.length else input.length
                repeat(count) { i ->
                    val filled = i < input.length
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
        when {
            numeric -> {
                NumberPad(
                    onDigit = { d -> if (input.length < lock.length) input += d },
                    onDelete = { input = input.dropLast(1) },
                    firstRequester = padRequester,
                )
                Tip(stringResource(R.string.accounts_unlock_tip_numeric))
            }

            dpad -> Tip(stringResource(R.string.accounts_unlock_tip_dpad))

            else -> {
                ArrowPad(
                    onKey = { input += it },
                    onDelete = { input = input.dropLast(1) },
                    onConfirm = submit,
                    firstRequester = padRequester,
                )
                Tip(stringResource(R.string.accounts_unlock_tip_direction))
            }
        }
        BvButton(
            text = stringResource(R.string.accounts_list_cancel),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            style = BvButtonStyle.Outlined,
        )
    }
}

@Composable
private fun Tip(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        style = BvType.Label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 数字键盘：1–9、删除、0。 */
@Composable
private fun NumberPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, firstRequester: FocusRequester) {
    val rows = listOf("123", "456", "789")
    Column(
        Modifier
            .fillMaxWidth()
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { c, d ->
                    BvButton(
                        text = d.toString(),
                        onClick = { onDigit(d) },
                        modifier = if (r == 0 && c == 0) Modifier.focusRequester(firstRequester) else Modifier,
                        style = BvButtonStyle.Tonal,
                        horizontalPadding = 22.dp,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BvIconButton(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(R.string.accounts_unlock_delete),
                onClick = onDelete,
                style = BvIconButtonStyle.Standard,
            )
            BvButton(
                text = "0",
                onClick = { onDigit('0') },
                style = BvButtonStyle.Tonal,
                horizontalPadding = 22.dp,
            )
        }
    }
}

/** 方向键盘：↑ ↓ ← →、删除、确认。 */
@Composable
private fun ArrowPad(
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    firstRequester: FocusRequester,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BvIconButton(
            icon = Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            onClick = { onKey('u') },
            modifier = Modifier.focusRequester(firstRequester),
            style = BvIconButtonStyle.Tonal,
            size = 48.dp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BvIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                onClick = { onKey('l') },
                style = BvIconButtonStyle.Tonal,
                size = 48.dp,
            )
            BvIconButton(
                icon = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.accounts_unlock_confirm),
                onClick = onConfirm,
                style = BvIconButtonStyle.Filled,
                size = 48.dp,
            )
            BvIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                onClick = { onKey('r') },
                style = BvIconButtonStyle.Tonal,
                size = 48.dp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BvIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                onClick = { onKey('d') },
                style = BvIconButtonStyle.Tonal,
                size = 48.dp,
            )
            BvIconButton(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(R.string.accounts_unlock_delete),
                onClick = onDelete,
                size = 48.dp,
            )
        }
    }
}
