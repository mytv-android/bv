package com.github.mytv.bv.ui.designsystem.component

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.focus.bvFocus
import com.github.mytv.bv.ui.designsystem.theme.BvType

/**
 * 原型 `.m-search`：高 48（电视 52），全圆角，surfaceContainerHigh（输入状态 surfaceContainerHighest），
 * 左内边距 16 右 6；搜索图标 24 + 文字 + 36dp 麦克风按钮（语音识别不可用时隐藏）。
 *
 * onValueChange == null 时为只读入口（点击 → onClick，例如首页顶栏）；否则是可编辑输入框。
 */
@Composable
fun BvSearchField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null,
    onVoiceResult: ((String) -> Unit)? = null,
    active: Boolean = onValueChange != null,
    autoFocus: Boolean = false,
    height: Dp = 48.dp,
    textStyle: TextStyle = BvType.Body,
    trailing: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val voiceAvailable = remember(context) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.packageManager) != null
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text ->
                onVoiceResult?.invoke(text) ?: onValueChange?.invoke(text)
            }
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus && onValueChange != null) runCatching { focusRequester.requestFocus() } }

    val container = if (active) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(start = 16.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (onValueChange == null) {
                    Text(
                        value.ifEmpty { placeholder },
                        style = textStyle,
                        color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke(value) }),
                    )
                }
            }
            trailing?.invoke()
            if (voiceAvailable && (onVoiceResult != null || onValueChange != null)) {
                BvIconButton(
                    icon = Icons.Rounded.Mic,
                    contentDescription = stringResource(R.string.ds_voice_search),
                    onClick = {
                        voiceLauncher.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        )
                    },
                    size = 36.dp,
                )
            }
        }
    }
    if (onClick != null && onValueChange == null) {
        Surface(
            onClick = onClick,
            modifier = modifier.bvFocus(scale = 1.04f),
            shape = CircleShape,
            color = container,
            interactionSource = interaction,
        ) { content() }
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = if (focused) MaterialTheme.colorScheme.surfaceContainerHighest else container,
        ) { content() }
    }
}
