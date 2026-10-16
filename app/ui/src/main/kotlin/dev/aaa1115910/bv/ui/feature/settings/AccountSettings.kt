package dev.aaa1115910.bv.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.UserSwitchViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 「账号与安全」：多账号管理（AccountsRoute）、用户锁（UserLockRoute）、无痕模式（Prefs.incognitoMode）。
 */
@Composable
internal fun AccountSettings(onAccounts: () -> Unit, onUserLock: () -> Unit) {
    val userSwitch: UserSwitchViewModel = koinViewModel()
    var incognito by remember { mutableStateOf(Prefs.incognitoMode) }
    LaunchedEffect(Unit) { userSwitch.updateData() }

    val locked = userSwitch.currentUser.lock.isNotBlank()
    SettingRows(stringResource(R.string.settings_account_group)) {
        SettingNavRow(
            title = stringResource(R.string.settings_account_manage),
            subtitle = stringResource(R.string.settings_account_count, userSwitch.userDbList.size),
            onClick = onAccounts,
        )
        SettingNavRow(
            title = stringResource(R.string.settings_account_lock),
            subtitle = stringResource(
                if (locked) R.string.settings_account_lock_on else R.string.settings_account_lock_off
            ),
            onClick = onUserLock,
        )
    }

    SettingSwitchRow(
        title = stringResource(R.string.settings_account_incognito),
        subtitle = stringResource(R.string.settings_account_incognito_desc),
        checked = incognito,
        onCheckedChange = {
            incognito = it
            Prefs.incognitoMode = it
        },
    )
}
