package dev.aaa1115910.bv.ui.feature.dynamic

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.CardFrame
import dev.aaa1115910.bv.ui.designsystem.component.DurationBadge
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType

/** 正文 13sp 行高 1.55。 */
internal val DynamicBodyStyle = TextStyle(fontSize = 13.sp, lineHeight = 20.sp)
private val DynamicTitleStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
private val DynamicNameStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)

/**
 * 动态卡（原型 dynCard）：Column(padding 14, 圆角 24, surfaceContainerLow / 选中 secondaryContainer, spacedBy 10)：
 * 头部（头像 32 + 名字 + 「时间 · 动作」）、内容（视频 / 图文 / 文字 / 转发 / 直播 / 番剧 / 专栏）、底部（转发 · 评论 · 点赞）。
 */
@Composable
internal fun DynamicCard(
    item: DynamicItem,
    onClick: () -> Unit,
    onOpenOrig: (DynamicItem) -> Unit,
    onPictures: (List<Picture>, Int) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "dynamicCard",
    )
    Column(
        modifier
            .fillMaxWidth()
            .bvFocus(scale = 1.02f, corner = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DynamicHeader(item)
        DynamicBody(item, onOpenOrig = onOpenOrig, onPictures = onPictures)
        item.footer?.let { DynamicFooter(it) }
    }
}

/** 头部 Row(spacedBy 10)：BvAvatar + Column(名字 13sp Bold + Meta「{时间} · 投稿了视频」)。 */
@Composable
internal fun DynamicHeader(
    item: DynamicItem,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 32.dp,
    nameStyle: TextStyle = DynamicNameStyle,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvAvatar(url = item.author.avatar, size = avatarSize, fallbackText = item.author.author)
        Column(Modifier.weight(1f)) {
            Text(
                item.author.author,
                style = nameStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.metaText(),
                style = BvType.Meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 动态内容（旧手机 DynamicContent 支持的所有类型）。[full] = 详情页（正文不截断、图片全部显示）。
 */
@Composable
internal fun DynamicBody(
    item: DynamicItem,
    onOpenOrig: (DynamicItem) -> Unit,
    onPictures: (List<Picture>, Int) -> Unit,
    full: Boolean = false,
) {
    when (item.type) {
        DynamicType.Av -> item.video?.let { DynamicVideoBody(it) }
        DynamicType.Pgc -> item.pgc?.let { DynamicPgcBody(it) }
        DynamicType.Draw -> item.draw?.let { DynamicDrawBody(it, onPictures, full) }
        DynamicType.Word -> item.word?.let { DynamicText(it.text, full) }
        DynamicType.Forward -> DynamicForwardBody(item, onOpenOrig, onPictures, full)
        DynamicType.LiveRcmd -> item.liveRcmd?.let { DynamicLiveBody(it) }
        DynamicType.Article -> item.article?.let { DynamicArticleBody(it, full) }
        DynamicType.None -> item.none?.let {
            DynamicText(it.text, full, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DynamicType.UgcSeason -> Unit
    }
}

@Composable
internal fun DynamicText(text: String, full: Boolean = false, color: Color = MaterialTheme.colorScheme.onSurface) {
    if (text.isBlank()) return
    Text(
        text,
        style = DynamicBodyStyle,
        color = color,
        maxLines = if (full) Int.MAX_VALUE else 6,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 视频：标题 13sp 2 行 + 内嵌卡 Row(padding 8, 圆角 16, surfaceContainerHigh, spacedBy 10)(封面 120 16:10 圆角 10 + 时长 | 播放 / 弹幕)。 */
@Composable
private fun DynamicVideoBody(video: DynamicItem.DynamicVideoModule) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            video.title,
            style = DynamicTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                AsyncImage(
                    model = video.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (video.duration.isNotBlank()) {
                    DurationBadge(video.duration, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                statText(video.play, R.string.ds_play_count)?.let { MetaLine(it) }
                statText(video.danmaku, R.string.ds_danmaku_count)?.let { MetaLine(it) }
            }
        }
    }
}

@Composable
private fun MetaLine(text: String) {
    Text(
        text,
        style = BvType.Meta,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

/** 番剧：同视频的内嵌卡（封面 120 + 标题）。 */
@Composable
private fun DynamicPgcBody(pgc: DynamicItem.DynamicPgcModule) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = pgc.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Text(
            pgc.title,
            modifier = Modifier.weight(1f),
            style = DynamicTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 图文：（标题）+ 正文 13sp 行高 1.55 + 3 列方图网格（spacedBy 4，圆角 10），点图片进入图片查看器。 */
@Composable
private fun DynamicDrawBody(
    draw: DynamicItem.DynamicDrawModule,
    onPictures: (List<Picture>, Int) -> Unit,
    full: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        draw.title?.takeIf { it.isNotBlank() }?.let { title ->
            Text(
                title,
                style = DynamicBodyStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DynamicText(draw.text, full)
        if (draw.images.isNotEmpty()) {
            DynamicImageGrid(draw.images, onPictures, max = if (full) draw.images.size else 9)
        }
    }
}

/** 3 列方图网格；超过 [max] 张时最后一张显示「+N」。 */
@Composable
internal fun DynamicImageGrid(
    images: List<Picture>,
    onPictures: (List<Picture>, Int) -> Unit,
    max: Int = 9,
) {
    val shown = images.take(max.coerceAtLeast(1))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        shown.chunked(3).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                chunk.forEachIndexed { column, picture ->
                    val index = row * 3 + column
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .bvFocus(scale = 1.05f, corner = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onPictures(images, index) }
                    ) {
                        AsyncImage(
                            model = picture.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (index == shown.lastIndex && images.size > shown.size) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = .45f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(R.string.dynamic_more_images, images.size - shown.size),
                                    style = BvType.Section,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 转发：转发语 + 原动态（surfaceContainer 圆角 16，「@作者」+ 原动态内容），点原动态打开原动态。 */
@Composable
private fun DynamicForwardBody(
    item: DynamicItem,
    onOpenOrig: (DynamicItem) -> Unit,
    onPictures: (List<Picture>, Int) -> Unit,
    full: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item.word?.text?.let { DynamicText(it, full) }
        val orig = item.orig
        if (orig == null || orig.type == DynamicType.None) {
            Text(
                orig?.none?.text?.takeIf { it.isNotBlank() } ?: stringResource(R.string.dynamic_source_missing),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
                style = BvType.Small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .bvFocus(scale = 1.02f, corner = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onOpenOrig(orig) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (orig.author.mid != -1L && orig.author.author.isNotBlank()) {
                    Text(
                        stringResource(R.string.dynamic_at, orig.author.author),
                        style = BvType.Label.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DynamicBody(orig, onOpenOrig = onOpenOrig, onPictures = onPictures, full = full)
            }
        }
    }
}

/** 直播：标题 + 封面 16:10（左上「直播」角标）。 */
@Composable
private fun DynamicLiveBody(live: DynamicItem.DynamicLiveRcmdModule) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            live.title,
            style = DynamicTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = live.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            LiveBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
        }
    }
}

@Composable
internal fun LiveBadge(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.dynamic_live),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = BvType.Badge,
        color = MaterialTheme.colorScheme.onPrimary,
    )
}

/** 专栏：标题 Bold + 摘要 + 第一张封面。 */
@Composable
private fun DynamicArticleBody(article: DynamicItem.DynamicArticleModule, full: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            article.title,
            style = DynamicBodyStyle.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        DynamicText(article.text, full)
        article.covers.firstOrNull()?.takeIf { it.isNotBlank() }?.let { cover ->
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}

/** 底部 Row(SpaceAround) 12sp onSurfaceVariant：转发 / 评论 N / 点赞 N（图标 14dp，仅显示：旧版点赞未实现）。 */
@Composable
internal fun DynamicFooter(footer: DynamicItem.DynamicFooterModule, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FooterStat(Icons.Outlined.Share, stringResource(R.string.dynamic_forward))
        FooterStat(Icons.AutoMirrored.Outlined.Comment, formatCount(footer.comment))
        FooterStat(Icons.Outlined.ThumbUp, formatCount(footer.like))
    }
}

@Composable
private fun FooterStat(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text,
            style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 电视动态卡（原型 dynTv）：width 240，Column(spacedBy 7)：Row(头像 26 + 名字 12sp SemiBold + Meta「· N 小时前」)
 * + 封面 16:10（CardFrame，焦点效果同 VideoCard）+ 标题 2 行。
 */
@Composable
internal fun DynamicTvCard(item: DynamicItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val effective = item.effective()
    val time = item.author.pubTime.trim()
    Column(
        modifier.width(240.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvAvatar(url = item.author.avatar, size = 26.dp, fallbackText = item.author.author)
            Text(
                item.author.author,
                modifier = Modifier.weight(1f, fill = false),
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (time.isNotEmpty()) {
                Text(
                    stringResource(R.string.dynamic_time_ago, time),
                    style = BvType.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        CardFrame(
            cover = item.coverUrl(),
            aspectRatio = 16f / 10f,
            onClick = onClick,
            coverContent = {
                when (effective.type) {
                    DynamicType.Av -> effective.video?.duration?.takeIf { it.isNotBlank() }?.let {
                        DurationBadge(it, Modifier.align(Alignment.BottomEnd).padding(6.dp))
                    }

                    DynamicType.Draw -> effective.draw?.images?.size?.takeIf { it > 1 }?.let {
                        DurationBadge(
                            stringResource(R.string.dynamic_images_count, it),
                            Modifier.align(Alignment.BottomEnd).padding(6.dp),
                        )
                    }

                    DynamicType.LiveRcmd -> LiveBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
                    else -> Unit
                }
            },
        ) {
            Text(
                item.titleText().orEmpty(),
                style = BvType.CardTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
