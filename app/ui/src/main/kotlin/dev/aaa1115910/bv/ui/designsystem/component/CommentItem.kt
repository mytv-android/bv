package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.theme.BvType

/**
 * 原型 `comment()`：Sunny 头像 30 + 名字（12sp Bold onSurfaceVariant）+ 正文（13sp 行高 1.5，表情内联）
 * + 图片 + 底行（点赞 · 时间 · 回复）。
 */
@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier = Modifier,
    likes: String? = null,
    maxLines: Int = 6,
    onPictureClick: ((pictures: List<Picture>, index: Int) -> Unit)? = null,
    onReplyClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
    /** 正文展开状态；为 null 时由组件内部托管（触屏点击「展开/收起」），电视端由评论面板托管以便遥控器触发。 */
    textExpanded: Boolean? = null,
    onToggleTextExpand: (() -> Unit)? = null,
    /** 触屏长按整条评论（带链接评论的二维码入口）；为 null 时不处理触摸长按。 */
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {},
                        onLongClick = onLongClick,
                    )
                } else Modifier
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BvAvatar(
            url = comment.member.avatar,
            size = 30.dp,
            fallbackText = comment.member.name,
            shape = AvatarShape.Sunny,
            modifier = if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (comment.isPinned) {
                    Text(
                        stringResource(R.string.ds_pinned),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        style = BvType.Meta.copy(lineHeight = 13.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    comment.member.name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = BvType.Label.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CommentText(comment.content, comment.emotes, maxLines, textExpanded, onToggleTextExpand)
            if (comment.pictures.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    comment.pictures.take(3).forEachIndexed { index, picture ->
                        AsyncImage(
                            model = picture.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(88.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(
                                    if (onPictureClick != null) Modifier.clickable { onPictureClick(comment.pictures, index) }
                                    else Modifier
                                ),
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (likes != null) {
                    Icon(
                        Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(likes, style = BvType.Meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (comment.timeDesc.isNotEmpty()) {
                    Text(comment.timeDesc, style = BvType.Meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val replyText = if (comment.repliesCount > 0) {
                    stringResource(R.string.ds_reply_count, comment.repliesCount)
                } else {
                    stringResource(R.string.ds_reply)
                }
                Text(
                    replyText,
                    modifier = if (onReplyClick != null) Modifier.clickable(onClick = onReplyClick) else Modifier,
                    style = BvType.Meta,
                    color = if (comment.repliesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentText(
    content: List<String>,
    emotes: List<Comment.Emote>,
    maxLines: Int,
    textExpanded: Boolean?,
    onToggleTextExpand: (() -> Unit)?,
) {
    val emoteNames = remember(emotes) { emotes.map { it.text }.toSet() }
    val inline = remember(emotes) {
        emotes.associate { emote ->
            emote.text to InlineTextContent(
                Placeholder(
                    width = (emote.size.fontSize / 13f).em,
                    height = (emote.size.fontSize / 13f).em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                )
            ) { AsyncImage(model = emote.url, contentDescription = null) }
        }
    }
    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = textExpanded ?: internalExpanded
    var overflow by remember { mutableStateOf(false) }
    Text(
        buildAnnotatedString {
            content.forEach { if (it in emoteNames) appendInlineContent(it) else append(it) }
        },
        style = BvType.Small.copy(lineHeight = 19.5.sp),
        color = MaterialTheme.colorScheme.onSurface,
        inlineContent = inline,
        maxLines = if (expanded) Int.MAX_VALUE else maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow) overflow = true },
    )
    if (overflow) {
        Text(
            stringResource(if (expanded) R.string.ds_collapse else R.string.ds_expand),
            modifier = Modifier.clickable {
                if (textExpanded == null) internalExpanded = !internalExpanded
                else onToggleTextExpand?.invoke()
            },
            style = BvType.Label.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
