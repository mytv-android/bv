package com.github.mytv.bv.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.m3qrcode.MaterialShapeQr

private val commentLinkRegex = Regex("""https?://[^\s<>"']+""")

/** 链接末尾常见的标点（中文评论里链接后面经常直接跟标点）。 */
private val trailingPunctuation = charArrayOf(
    '。', '，', '！', '？', '；', '：', '、', '）', '】', '》', '」', '』', '”', '’',
    ',', '.', ';', ':', '!', '?', ')', ']', '}',
)

private const val MAX_QR_LINK_LENGTH = 512

/**
 * 提取评论正文中的第一条可生成二维码的 http(s) 链接。
 * 无链接、链接过长或 zxing 无法编码（例如超出二维码容量）时返回 null。
 */
internal fun commentQrLink(content: List<String>): String? {
    val text = content.joinToString("")
    return commentLinkRegex.findAll(text)
        .map { it.value.trimEnd(*trailingPunctuation) }
        .firstOrNull(::canEncodeAsQr)
}

private fun canEncodeAsQr(link: String): Boolean =
    link.length <= MAX_QR_LINK_LENGTH &&
        runCatching { Encoder.encode(link, ErrorCorrectionLevel.M, null) }.isSuccess

/**
 * 评论链接二维码：全屏遮罩内的对话框，二维码下方展示原始链接；返回键关闭。
 * 复用 app/shared 内置的 MaterialShapeQr，不引入新依赖。
 */
@Composable
internal fun CommentQrCodeDialog(link: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            val qrSize = minOf(maxWidth * 0.42f, maxHeight * 0.45f, 360.dp)
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(0.8f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ds_comment_qr_title),
                        style = BvType.PanelTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .size(qrSize)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MaterialShapeQr(
                            modifier = Modifier.fillMaxSize(),
                            content = link,
                        )
                    }
                    Text(
                        text = link,
                        style = BvType.Small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.ds_comment_qr_hint),
                        style = BvType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
