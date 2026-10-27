package com.github.mytv.bv.ui.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.AvatarShape
import com.github.mytv.bv.ui.designsystem.component.BvAvatar
import com.github.mytv.bv.ui.designsystem.component.BvButton
import com.github.mytv.bv.ui.designsystem.component.toComposeShape
import com.github.mytv.bv.ui.designsystem.theme.BvType
import java.util.Locale

/** 数量：≥ 1 万显示「1.2万」。 */
@Composable
internal fun formatCount(value: Number): String {
    val n = value.toDouble()
    return if (n >= 10_000) {
        val wan = n / 10_000
        val text = if (wan >= 100) wan.toLong().toString() else String.format(Locale.ROOT, "%.1f", wan).removeSuffix(".0")
        stringResource(R.string.me_count_wan, text)
    } else if (n == kotlin.math.floor(n)) {
        n.toLong().toString()
    } else {
        String.format(Locale.ROOT, "%.1f", n)
    }
}

/**
 * 资料头（原型 profileHead）：Row(spacedBy 16)：BvAvatar(72) + Column(spacedBy 6)：
 * Row(spacedBy 8)(昵称 PanelTitle + 等级徽章 30dp Sunny secondaryContainer 数字 11sp) + 12sp「关注 · 粉丝 · 硬币」。
 */
@Composable
internal fun ProfileHeader(
    face: String?,
    name: String,
    level: Int?,
    stats: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvAvatar(url = face?.takeIf { it.isNotBlank() }, size = 72.dp, fallbackText = name)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = BvType.PanelTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (level != null) LevelBadge(level)
            }
            if (stats != null) {
                Text(
                    stats,
                    style = BvType.Meta.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 等级徽章：30dp Sunny 形状，secondaryContainer，数字 11sp Bold。 */
@Composable
private fun LevelBadge(level: Int) {
    val label = stringResource(R.string.me_level, level)
    Box(
        Modifier
            .size(30.dp)
            .clip(AvatarShape.Sunny.toComposeShape())
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            level.toString(),
            style = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** 未登录：头像占位（72）+「登录后同步历史、收藏和追番」+「登录」→ LoginRoute。 */
@Composable
internal fun LoggedOutHeader(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    loginRequester: FocusRequester? = null,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            BvAvatar(url = null, size = 72.dp)
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.me_login_hint),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BvButton(
                text = stringResource(R.string.me_login),
                onClick = onLogin,
                modifier = if (loginRequester != null) Modifier.focusRequester(loginRequester) else Modifier,
            )
        }
    }
}

/** 已登录显示资料头，未登录显示登录提示。 */
@Composable
internal fun MeHeader(
    state: MeState,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    loginRequester: FocusRequester? = null,
) {
    if (!state.isLogin) {
        LoggedOutHeader(onLogin = onLogin, modifier = modifier, loginRequester = loginRequester)
        return
    }
    val info = state.info
    val stats = info?.let {
        stringResource(
            R.string.me_profile_stats,
            formatCount(it.following),
            formatCount(it.follower),
            formatCount(it.coins),
        )
    }
    ProfileHeader(
        face = info?.face ?: state.user.face,
        name = info?.name ?: state.user.username,
        level = info?.level,
        stats = stats,
        modifier = modifier,
    )
}
