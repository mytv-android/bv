package com.github.mytv.bv.ui.feature.accounts

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.db.UserDB
import com.github.mytv.bv.repository.UserRepository
import com.github.mytv.bv.ui.designsystem.adaptive.BvInputMode
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.AvatarShape
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.BvButtonStyle
import com.github.mytv.bv.ui.designsystem.component.toComposeShape
import com.github.mytv.bv.ui.designsystem.focus.BvKey
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.focus.focusContainerColor
import com.github.mytv.bv.ui.designsystem.focus.focusContentColor
import com.github.mytv.bv.ui.designsystem.focus.onBvKey
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.ui.feature.tools.ToolPage
import com.github.mytv.bv.ui.navigation.LoginRoute
import com.github.mytv.bv.viewmodel.UserSwitchViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val lockLogger = KotlinLogging.logger("BvUserLockSettings")

/** 触屏新密码的位数（与解锁界面 UnlockUser 的数字键盘一致：数字 lock 按其长度输入）。 */
private const val NUMERIC_LOCK_LENGTH = 4

private enum class LockStep { ChooseUser, InputOld, InputNew, ConfirmNew }

/** 方向键序列密码字符（与旧电视 UserLockSettingsScreen / 新 UnlockUser 相同：u d l r）。 */
private fun BvKey.toLockSettingChar(): Char? = when (this) {
    BvKey.Up -> 'u'
    BvKey.Down -> 'd'
    BvKey.Left -> 'l'
    BvKey.Right -> 'r'
    else -> null
}

private fun String.isNumericLockValue() = isNotEmpty() && all { it.isDigit() }

/** 方向序列显示为箭头（旧 UserLockSettingsContent.inputShow）。 */
private fun String.toArrows(): String = map {
    when (it) {
        'u' -> '↑'
        'd' -> '↓'
        'l' -> '←'
        'r' -> '→'
        else -> it
    }
}.joinToString("")

private fun FocusRequester.tryLockFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/**
 * 用户锁设置（第 9.11 节模板；逻辑照抄旧电视 UserLockSettingsScreen）：
 * 选择账号（只有一个账号时跳过）→ 已有密码时先输入旧密码 → 输入新密码 → 再次输入确认 → 保存到 UserDB.lock。
 * - 电视（方向键）：方向键输入 u/d/l/r 序列，确认键确定，返回键删除一位；新密码留空直接按确认键 = 移除用户锁。
 * - 触屏：4 位数字键盘（存同一个 lock 字段，UnlockUser 会按数字键盘解锁）；有「移除用户锁」按钮。
 * - 旧密码是数字时（在手机上设置的）电视也用数字键盘；触屏遇到电视上设置的方向序列时显示方向键盘。
 */
@Composable
fun UserLockScreen(nav: NavController) {
    val vm: UserSwitchViewModel = koinViewModel()
    val userRepository: UserRepository = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dpad = LocalDeviceProfile.current.input == BvInputMode.DPad
    val users = vm.userDbList

    var target by remember { mutableStateOf<UserDB?>(null) }
    var step by remember { mutableStateOf(LockStep.ChooseUser) }
    var input by remember { mutableStateOf("") }
    var firstInput by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    val chooseRequester = remember { FocusRequester() }
    val captureRequester = remember { FocusRequester() }
    val padRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { vm.updateData() }

    val toast: (Int) -> Unit = { res -> Toast.makeText(context, res, Toast.LENGTH_SHORT).show() }
    val startFor: (UserDB) -> Unit = { user ->
        target = user
        input = ""
        firstInput = ""
        step = if (user.lock.isNotBlank()) LockStep.InputOld else LockStep.InputNew
    }
    // 只有一个账号时直接进入输入；多个账号时先选择（默认焦点在当前账号）
    LaunchedEffect(vm.loading, users.size) {
        if (!initialized && !vm.loading) {
            initialized = true
            if (users.size == 1) startFor(users.first())
        }
    }

    val save: (UserDB, String) -> Unit = { user, lock ->
        if (!saving) {
            saving = true
            scope.launch {
                runCatching { userRepository.updateUser(user.copy(lock = lock)) }
                    .onSuccess {
                        lockLogger.info { "update user lock for ${user.uid}" }
                        toast(if (lock.isEmpty()) R.string.accounts_lock_removed else R.string.accounts_lock_saved)
                        vm.updateData()
                        nav.popBackStack()
                    }
                    .onFailure {
                        lockLogger.warn(it) { "update user lock failed" }
                        toast(R.string.accounts_lock_save_failed)
                        saving = false
                    }
            }
        }
    }

    // 确认当前输入（旧 UserLockSettingsContent 的 Center 键逻辑）
    val submit: () -> Unit = submit@{
        val user = target ?: return@submit
        when (step) {
            LockStep.InputOld -> {
                if (input == user.lock) {
                    step = LockStep.InputNew
                } else {
                    toast(R.string.accounts_lock_wrong)
                }
                input = ""
            }

            LockStep.InputNew -> {
                if (input.isBlank()) {
                    // 电视：不输入直接确认 = 移除密码（旧行为）；触屏用「移除用户锁」按钮
                    if (dpad && user.lock.isNotBlank()) save(user, "")
                } else {
                    firstInput = input
                    input = ""
                    step = LockStep.ConfirmNew
                }
            }

            LockStep.ConfirmNew -> {
                if (input == firstInput) {
                    save(user, input)
                } else {
                    toast(R.string.accounts_lock_different)
                    input = ""
                    firstInput = ""
                    step = LockStep.InputNew
                }
            }

            LockStep.ChooseUser -> Unit
        }
    }

    val oldLock = target?.lock.orEmpty()
    // 当前步骤用哪种键盘
    val numericPad = when (step) {
        LockStep.InputOld -> oldLock.isNumericLockValue()
        LockStep.InputNew, LockStep.ConfirmNew -> !dpad
        LockStep.ChooseUser -> false
    }
    val padLength = if (step == LockStep.InputOld) oldLock.length else NUMERIC_LOCK_LENGTH

    // 数字密码输满后自动确认
    LaunchedEffect(input, step, numericPad) {
        if (numericPad && step != LockStep.ChooseUser && input.length >= padLength) submit()
    }
    // 焦点：选择账号 → 当前账号；数字键盘 → 第一个键；方向键序列 → 捕获框
    LaunchedEffect(step, users.size, numericPad) {
        withFrameNanos { }
        when {
            step == LockStep.ChooseUser -> if (users.isNotEmpty()) chooseRequester.tryLockFocus()
            numericPad -> padRequester.tryLockFocus()
            dpad -> captureRequester.tryLockFocus()
        }
    }

    // 返回键：删除一位 → 回到选择账号（多账号时）→ 退出页面
    BackHandler(enabled = !saving) {
        when {
            step != LockStep.ChooseUser && input.isNotEmpty() -> input = input.dropLast(1)
            step != LockStep.ChooseUser && users.size > 1 -> {
                step = LockStep.ChooseUser
                target = null
            }

            else -> nav.popBackStack()
        }
    }

    ToolPage(title = stringResource(R.string.accounts_lock_title), onBack = { nav.popBackStack() }) { insets ->
        when {
            vm.loading && users.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }

            users.isEmpty() -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(insets.padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text(
                    stringResource(R.string.accounts_lock_no_user),
                    style = BvType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                BvButton(
                    text = stringResource(R.string.accounts_lock_login),
                    onClick = { nav.navigate(LoginRoute) },
                    style = BvButtonStyle.Tonal,
                )
            }

            else -> Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(insets.padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier.widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(AvatarShape.Cookie.toComposeShape())
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        stringResource(
                            when (step) {
                                LockStep.ChooseUser -> R.string.accounts_lock_choose_user
                                LockStep.InputOld -> R.string.accounts_lock_input_old
                                LockStep.InputNew -> R.string.accounts_lock_input_new
                                LockStep.ConfirmNew -> R.string.accounts_lock_input_again
                            }
                        ),
                        style = BvType.PanelTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    val currentUid = vm.currentUser.uid
                    val initialUid = users.firstOrNull { it.uid == currentUid }?.uid ?: users.firstOrNull()?.uid
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .focusRestorer()
                            .focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        users.forEach { user ->
                            val visible = step == LockStep.ChooseUser || user.uid == target?.uid
                            if (visible) {
                                LockUserItem(
                                    user = user,
                                    clickable = step == LockStep.ChooseUser && !saving,
                                    onClick = { startFor(user) },
                                    modifier = if (user.uid == initialUid) Modifier.focusRequester(chooseRequester) else Modifier,
                                )
                            }
                        }
                    }
                    if (step != LockStep.ChooseUser && target != null) {
                        when {
                            numericPad -> {
                                LockPinDots(total = padLength, filled = input.length)
                                LockNumericPad(
                                    onDigit = { d -> if (input.length < padLength) input += d },
                                    onDelete = { input = input.dropLast(1) },
                                    firstRequester = padRequester,
                                )
                                LockTip(stringResource(R.string.accounts_lock_tip_numeric, padLength))
                            }

                            dpad -> {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .focusRequester(captureRequester)
                                        .onBvKey { key ->
                                            val char = key.toLockSettingChar()
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
                                        .focusable(),
                                    contentAlignment = Alignment.Center,
                                ) { LockInputText(input.toArrows()) }
                                LockTip(stringResource(R.string.accounts_lock_tip_dpad))
                                if (step == LockStep.InputNew && oldLock.isNotBlank()) {
                                    LockTip(stringResource(R.string.accounts_lock_tip_dpad_remove))
                                }
                            }

                            else -> {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    contentAlignment = Alignment.Center,
                                ) { LockInputText(input.toArrows()) }
                                LockDirectionPad(
                                    onKey = { input += it },
                                    onDelete = { input = input.dropLast(1) },
                                    onConfirm = submit,
                                    firstRequester = padRequester,
                                )
                                LockTip(stringResource(R.string.accounts_lock_tip_direction))
                            }
                        }
                        // 触屏：已有密码时可直接移除
                        if (!dpad && step == LockStep.InputNew && oldLock.isNotBlank()) {
                            BvButton(
                                text = stringResource(R.string.accounts_lock_remove),
                                onClick = { target?.let { save(it, "") } },
                                style = BvButtonStyle.Outlined,
                                enabled = !saving,
                            )
                        }
                    }
                    if (saving) LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun LockTip(text: String) {
    Text(
        text,
        style = BvType.Label.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** 已输入的方向序列（箭头）。 */
@Composable
private fun LockInputText(text: String) {
    Text(
        text,
        style = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 数字密码的位数点：已输入 primary 实心，未输入 outline 空心。 */
@Composable
private fun LockPinDots(total: Int, filled: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { index ->
            val on = index < filled
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (on) scheme.primary else Color.Transparent)
                    .border(2.dp, if (on) scheme.primary else scheme.outline, CircleShape)
            )
        }
    }
}

/** 账号项：头像 64（已上锁的账号右下角带锁）+ 名字。 */
@Composable
private fun LockUserItem(
    user: UserDB,
    clickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .width(96.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box {
                BvAvatar(url = user.avatar, size = 64.dp, fallbackText = user.username)
                if (user.lock.isNotBlank()) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(scheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Text(
                user.username,
                style = BvType.CardTitle,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (clickable) {
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        val container by focusContainerColor(focused, Color.Transparent)
        val content by focusContentColor(focused, scheme.onSurface)
        Surface(
            onClick = onClick,
            modifier = modifier.bvFocus(corner = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = container,
            contentColor = content,
            interactionSource = interaction,
        ) { body() }
    } else {
        Box(modifier.alpha(1f)) { body() }
    }
}

/** 键盘键：高 52，圆角 16，surfaceContainerHigh；方向键聚焦放大 + 描边 + inverseSurface。 */
@Composable
private fun LockPadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    icon: ImageVector? = null,
    text: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val container by focusContainerColor(focused, MaterialTheme.colorScheme.surfaceContainerHigh)
    val content by focusContentColor(focused, MaterialTheme.colorScheme.onSurface)
    Surface(
        onClick = onClick,
        modifier = modifier
            .bvFocus(corner = 16.dp)
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
            } else if (text != null) {
                Text(
                    text,
                    style = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

/** 数字键盘：1–9、0、删除。 */
@Composable
private fun LockNumericPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, firstRequester: FocusRequester) {
    Column(
        Modifier
            .width(264.dp)
            .focusRestorer()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit ->
                    LockPadKey(
                        onClick = { onDigit(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .then(if (digit == '1') Modifier.focusRequester(firstRequester) else Modifier),
                        text = digit.toString(),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            LockPadKey(onClick = { onDigit('0') }, modifier = Modifier.weight(1f), text = "0")
            LockPadKey(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(R.string.accounts_lock_delete),
            )
        }
    }
}

/** 触屏输入方向序列（旧密码是电视上设置的）：↑ ← ✓ → ↓ 与删除。 */
@Composable
private fun LockDirectionPad(
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    firstRequester: FocusRequester,
) {
    Column(
        Modifier
            .width(216.dp)
            .focusRestorer()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            LockPadKey(
                onClick = { onKey('u') },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(firstRequester),
                icon = Icons.Rounded.KeyboardArrowUp,
            )
            Spacer(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LockPadKey(onClick = { onKey('l') }, modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft)
            LockPadKey(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.accounts_lock_confirm),
            )
            LockPadKey(onClick = { onKey('r') }, modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LockPadKey(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(R.string.accounts_lock_delete),
            )
            LockPadKey(onClick = { onKey('d') }, modifier = Modifier.weight(1f), icon = Icons.Rounded.KeyboardArrowDown)
            Spacer(Modifier.weight(1f))
        }
    }
}
