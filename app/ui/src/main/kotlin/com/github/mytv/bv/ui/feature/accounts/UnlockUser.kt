package com.github.mytv.bv.ui.feature.accounts

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.entity.db.UserDB
import com.github.mytv.bv.ui.designsystem.adaptive.BvInputMode
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.AvatarShape
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.toComposeShape
import com.github.mytv.bv.ui.designsystem.focus.BvKey
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.focus.focusContainerColor
import com.github.mytv.bv.ui.designsystem.focus.focusContentColor
import com.github.mytv.bv.ui.designsystem.focus.onBvKey
import com.github.mytv.bv.ui.designsystem.theme.BvType
import com.github.mytv.bv.viewmodel.UserSwitchViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val unlockLogger = KotlinLogging.logger("BvUnlockUser")

private enum class UnlockStep { ChooseUser, InputPassword }

/** 方向键序列密码的字符（与旧电视 UnlockUserScreen / UserLockSettingsScreen 相同：u d l r）。 */
private fun BvKey.toLockChar(): Char? = when (this) {
    BvKey.Up -> 'u'
    BvKey.Down -> 'd'
    BvKey.Left -> 'l'
    BvKey.Right -> 'r'
    else -> null
}

/** 触屏上用 4 位数字密码（存同一个 lock 字段）；全是数字的 lock 用数字键盘输入。 */
private fun String.isNumericLock() = isNotEmpty() && all { it.isDigit() }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun FocusRequester.tryFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/**
 * 启动时的用户锁解锁界面（第 8.1 节；逻辑照抄旧电视 UnlockUserScreen + UnlockSwitchUserContent）：
 * 先选择账号（没有锁的账号直接切换进入），再输入该账号的密码；比较方式与旧版相同（lock == 输入）。
 * - 电视（方向键）：方向键输入 u/d/l/r 序列，确认键解锁，返回键删除一位 / 回到选择账号；
 * - 触屏：4 位数字键盘（lock 为数字时，输满自动校验）；lock 是电视上设置的方向序列时显示方向键盘。
 * 样式：居中卡片（圆角 28，surfaceContainer）。
 */
@Composable
internal fun UnlockUserScreen(onUnlocked: () -> Unit) {
    val vm: UserSwitchViewModel = koinViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dpad = LocalDeviceProfile.current.input == BvInputMode.DPad
    val users = vm.userDbList
    var selected by remember { mutableStateOf<UserDB?>(null) }
    var step by remember { mutableStateOf(UnlockStep.ChooseUser) }
    var input by remember { mutableStateOf("") }
    var unlocking by remember { mutableStateOf(false) }
    val chooseRequester = remember { FocusRequester() }
    val captureRequester = remember { FocusRequester() }
    val padRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { vm.updateData() }

    val unlock: (UserDB) -> Unit = { user ->
        if (!unlocking) {
            unlocking = true
            unlockLogger.info { "unlock user lock for user ${user.uid}" }
            scope.launch {
                runCatching { vm.switchUser(user) }
                    .onFailure { unlockLogger.warn(it) { "switch user failed" } }
                onUnlocked()
            }
        }
    }
    val submit: () -> Unit = {
        val user = selected
        if (user != null) {
            if (user.lock == input) {
                unlock(user)
            } else {
                Toast.makeText(context, R.string.accounts_unlock_wrong_password, Toast.LENGTH_SHORT).show()
                input = ""
            }
        }
    }
    val choose: (UserDB) -> Unit = { user ->
        selected = user
        input = ""
        if (user.lock.isNotBlank()) step = UnlockStep.InputPassword else unlock(user)
    }

    // 返回键：删除一位 → 回到选择账号 → 退出应用（不能绕过用户锁）
    BackHandler {
        when {
            step == UnlockStep.InputPassword && input.isNotEmpty() -> input = input.dropLast(1)
            step == UnlockStep.InputPassword -> step = UnlockStep.ChooseUser
            else -> context.findActivity()?.finish()
        }
    }

    val lock = selected?.lock.orEmpty()
    val numeric = lock.isNumericLock()
    LaunchedEffect(step, users.size) {
        withFrameNanos { }
        when {
            step == UnlockStep.ChooseUser -> if (users.isNotEmpty()) chooseRequester.tryFocus()
            numeric -> padRequester.tryFocus()
            dpad -> captureRequester.tryFocus()
        }
    }
    // 数字密码输满后自动校验
    LaunchedEffect(input, numeric) {
        if (step == UnlockStep.InputPassword && numeric && input.length >= lock.length) submit()
    }

    val scheme = MaterialTheme.colorScheme
    val initialUid = users.firstOrNull { it.uid == vm.currentUser.uid }?.uid ?: users.firstOrNull()?.uid
    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainer,
            contentColor = scheme.onSurface,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(AvatarShape.Cookie.toComposeShape())
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                }
                Text(
                    stringResource(
                        if (step == UnlockStep.ChooseUser) R.string.accounts_unlock_choose_user else R.string.accounts_unlock_input_password
                    ),
                    style = BvType.PanelTitle,
                )
                if (vm.loading && users.isEmpty()) {
                    LoadingIndicator()
                } else {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .focusRestorer()
                            .focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        users.forEach { user ->
                            UnlockUserItem(
                                user = user,
                                clickable = step == UnlockStep.ChooseUser && !unlocking,
                                dimmed = step == UnlockStep.InputPassword && user != selected,
                                onClick = { choose(user) },
                                modifier = if (user.uid == initialUid) Modifier.focusRequester(chooseRequester) else Modifier,
                            )
                        }
                    }
                }
                if (step == UnlockStep.InputPassword && selected != null) {
                    when {
                        numeric -> {
                            PinDots(total = lock.length, filled = input.length)
                            NumericPad(
                                onDigit = { d -> if (input.length < lock.length) input += d },
                                onDelete = { input = input.dropLast(1) },
                                firstRequester = padRequester,
                            )
                            Tip(stringResource(R.string.accounts_unlock_tip_numeric))
                        }

                        dpad -> {
                            // 方向键序列：焦点停在这里，方向键追加 u/d/l/r，确认键校验
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
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
                                    .focusable(),
                                contentAlignment = Alignment.Center,
                            ) { MaskedInput(input) }
                            Tip(stringResource(R.string.accounts_unlock_tip_dpad))
                        }

                        else -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                contentAlignment = Alignment.Center,
                            ) { MaskedInput(input) }
                            DirectionPad(
                                onKey = { input += it },
                                onDelete = { input = input.dropLast(1) },
                                onConfirm = submit,
                            )
                            Tip(stringResource(R.string.accounts_unlock_tip_direction))
                        }
                    }
                }
                if (unlocking) LoadingIndicator()
            }
        }
    }
}

@Composable
private fun Tip(text: String) {
    Text(
        text,
        style = BvType.Label.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** 已输入的位数（不显示内容）。 */
@Composable
private fun MaskedInput(input: String) {
    Text(
        "●".repeat(input.length),
        style = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 6.sp),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

/** 数字密码的位数点：已输入 primary 实心，未输入 outline 空心。 */
@Composable
private fun PinDots(total: Int, filled: Int) {
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

/** 账号项：头像 64（上锁的账号右下角带锁）+ 名字；输入密码时其他账号淡化为 0.4。 */
@Composable
private fun UnlockUserItem(
    user: UserDB,
    clickable: Boolean,
    dimmed: Boolean,
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
        Box(modifier.alpha(if (dimmed) .4f else 1f)) { body() }
    }
}

/** 键盘键：高 52，圆角 16，surfaceContainerHigh；方向键聚焦放大 + 描边 + inverseSurface。 */
@Composable
private fun PadKey(
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

/** 4 位数字密码键盘：1–9、0、删除。 */
@Composable
private fun NumericPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, firstRequester: FocusRequester) {
    val rows = listOf("123", "456", "789")
    Column(
        Modifier
            .width(264.dp)
            .focusRestorer()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit ->
                    PadKey(
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
            PadKey(onClick = { onDigit('0') }, modifier = Modifier.weight(1f), text = "0")
            PadKey(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(R.string.accounts_unlock_delete),
            )
        }
    }
}

/** 触屏输入方向序列密码（电视上设置的锁）：↑ ← ✓ → ↓ 与删除。 */
@Composable
private fun DirectionPad(onKey: (Char) -> Unit, onDelete: () -> Unit, onConfirm: () -> Unit) {
    Column(
        Modifier
            .width(216.dp)
            .focusRestorer()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            PadKey(onClick = { onKey('u') }, modifier = Modifier.weight(1f), icon = Icons.Rounded.KeyboardArrowUp)
            Spacer(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PadKey(onClick = { onKey('l') }, modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft)
            PadKey(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.accounts_unlock_confirm),
            )
            PadKey(onClick = { onKey('r') }, modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PadKey(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = stringResource(R.string.accounts_unlock_delete),
            )
            PadKey(onClick = { onKey('d') }, modifier = Modifier.weight(1f), icon = Icons.Rounded.KeyboardArrowDown)
            Spacer(Modifier.weight(1f))
        }
    }
}
