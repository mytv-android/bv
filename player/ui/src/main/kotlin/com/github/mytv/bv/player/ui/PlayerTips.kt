@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package com.github.mytv.bv.player.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.player.ui.design.PlayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import qrcode.QRCode
import qrcode.color.DefaultColorFunction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private val TipShape = RoundedCornerShape(28.dp)

/** 缓冲中：Expressive 加载指示器（旧版同款，120dp）。 */
@Composable
internal fun BufferingTip(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier = modifier.size(120.dp))
}

/** 付费内容试看提示：起播后短暂显示。 */
@Composable
internal fun PreviewTip(
    show: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.player_ui_preview_tip),
            modifier = Modifier
                .clip(TipShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            style = PlayerType.Body,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 播放出错提示（文案照搬旧电视版 PlayErrorTip），样式：surfaceContainer 圆角 28。 */
@Composable
internal fun PlayErrorTip(
    exception: Exception?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 480.dp)
            .clip(TipShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.player_ui_error_title),
            style = PlayerType.HeroPhone,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.player_ui_error_face),
            style = PlayerType.Body,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = stringResource(R.string.player_ui_error_message, exception?.message ?: ""),
            style = PlayerType.Small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 需要购买时的提示 + 购买页二维码（照搬旧电视版 PaidRequireTip）。 */
@Composable
internal fun PaidRequireTip(
    epid: Int,
    modifier: Modifier = Modifier,
) {
    var qrImage by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(epid) {
        val image = withContext(Dispatchers.IO) {
            runCatching {
                val output = ByteArrayOutputStream()
                val url = "https://b23.tv/ep$epid"
                QRCode(
                    data = url,
                    colorFn = DefaultColorFunction(
                        foreground = android.graphics.Color.WHITE,
                        background = android.graphics.Color.TRANSPARENT
                    )
                )
                    .render()
                    .writeImage(output)
                val input = ByteArrayInputStream(output.toByteArray())
                BitmapFactory.decodeStream(input).asImageBitmap()
            }.getOrNull()
        }
        qrImage = image
    }
    Column(
        modifier = modifier
            .clip(TipShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.player_ui_pay_title),
            style = PlayerType.HeroPhone,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // TODO 使用颜文字显示影片价格
        Text(
            text = stringResource(R.string.player_ui_pay_face),
            style = PlayerType.Body,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnimatedVisibility(visible = qrImage != null) {
            qrImage?.let {
                Image(
                    modifier = Modifier.padding(top = 6.dp),
                    bitmap = it,
                    contentDescription = "EP$epid QR Code"
                )
            }
        }
    }
}
