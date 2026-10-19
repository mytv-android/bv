package dev.aaa1115910.bv.ui.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.adaptive.BvInputMode
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.focus.focusRing
import dev.aaa1115910.bv.ui.designsystem.focus.rememberCardFocus
import dev.aaa1115910.bv.ui.designsystem.focus.showFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

/** 视频卡长按菜单的动作（「从这里开始刷」「稍后再看」「UP 主主页」），由应用外壳提供。 */
@Stable
interface VideoCardActions {
    fun openShorts(aid: Long)
    fun addToView(aid: Long)
    fun openUp(mid: Long)
}

val LocalVideoCardActions = staticCompositionLocalOf<VideoCardActions?> { null }

/** 原型 `.m-vc` + `vcard()`。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    data: VideoCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,              // 横向行里固定宽度；网格里传 null（填满格子）
    progress: Float? = null,        // 0..1，继续观看进度条
    showMeta: Boolean = true,       // UP 主 + 播放量
    showStats: Boolean = false,     // 封面底部「▶ 播放　弹 弹幕」渐变条
    large: Boolean = false,         // 列表行里 14sp 标题
    onLongClick: (() -> Unit)? = null,
) {
    val actions = LocalVideoCardActions.current
    var menu by remember { mutableStateOf(false) }
    val longClick = onLongClick ?: actions?.let { { menu = true } }
    CardFrame(
        cover = data.cover,
        aspectRatio = 16f / 10f,
        onClick = onClick,
        onLongClick = longClick,
        modifier = modifier,
        width = width,
        coverContent = {
            if (showStats && (data.playString.isNotEmpty() || data.danmakuString.isNotEmpty())) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f))))
                        .padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 6.dp)
                ) {
                    Text(
                        stringResource(R.string.ds_card_stats, data.playString, data.danmakuString),
                        style = BvType.Badge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
            if (data.timeString.isNotEmpty()) {
                DurationBadge(data.timeString, Modifier.align(Alignment.BottomEnd).padding(6.dp))
            }
            if (progress != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        },
    ) {
        Text(
            data.title,
            style = if (large) BvType.Body.copy(fontWeight = FontWeight.Medium) else BvType.CardTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showMeta) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (data.upName.isNotEmpty()) {
                    Text(
                        data.upName,
                        modifier = Modifier.weight(1f, fill = false),
                        style = BvType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (data.playString.isNotEmpty()) {
                    Text(
                        stringResource(R.string.ds_play_count, data.playString),
                        style = BvType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        if (actions != null) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ds_card_menu_shorts)) },
                    onClick = { menu = false; actions.openShorts(data.avid) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ds_card_menu_toview)) },
                    onClick = { menu = false; actions.addToView(data.avid) },
                )
                data.upMid?.let { mid ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ds_card_menu_up)) },
                        onClick = { menu = false; actions.openUp(mid) },
                    )
                }
            }
        }
    }
}

/** 时长角标：右下 6dp，内边距 6×2，圆角 8，黑 α0.55，等宽数字。 */
@Composable
fun DurationBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = .55f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = BvType.Badge.copy(fontFeatureSettings = "tnum"),
        color = Color.White,
    )
}

/**
 * 卡片骨架：封面 + 文字。整张卡可点击，聚焦（方向键）时整卡放大 1.08，只有封面描边，封面圆角 16→22。
 * 其他「封面在上、文字在下」的卡片（分 P 卡、动态电视卡）也用它。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardFrame(
    cover: String?,
    aspectRatio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    coverContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focus = rememberCardFocus(focused)
    val scale by animateFloatAsState(
        if (focus.show) 1.08f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "cardScale"
    )
    val touch = LocalDeviceProfile.current.input == BvInputMode.Touch
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .zIndex(if (focus.show) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .focusRing(focus.show, { focus.corner.value })
                .graphicsLayer {
                    shape = RoundedCornerShape(focus.corner.value)
                    clip = true
                }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(if (touch) Modifier.indication(interaction, ripple()) else Modifier)
        ) {
            if (!cover.isNullOrEmpty()) {
                AsyncImage(
                    // 卡片封面按 640w 小图加载（原图可能 2K/4K）；已带处理参数的 URL 不会二次追加
                    model = cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            coverContent()
        }
        content()
    }
}

/** 列表里的横向视频行（相关推荐、搜索结果）：封面 128 宽 16:10 圆角 12 + 标题 2 行 + 副信息。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoRow(
    data: VideoCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverWidth: Dp = 128.dp,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val show = showFocus(focused)
    val actions = LocalVideoCardActions.current
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (show) 1f else 0f)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick ?: actions?.let { { menu = true } },
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .width(coverWidth)
                .aspectRatio(16f / 10f)
                .focusRing(show, 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .indication(interaction, ripple())
        ) {
            AsyncImage(
                model = data.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (data.timeString.isNotEmpty()) {
                DurationBadge(data.timeString, Modifier.align(Alignment.BottomEnd).padding(6.dp))
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                data.title,
                style = BvType.CardTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                data.upName.takeIf { it.isNotEmpty() },
                data.playString.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.ds_play_count, it) },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    style = BvType.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (actions != null) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ds_card_menu_shorts)) },
                    onClick = { menu = false; actions.openShorts(data.avid) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ds_card_menu_toview)) },
                    onClick = { menu = false; actions.addToView(data.avid) },
                )
                data.upMid?.let { mid ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ds_card_menu_up)) },
                        onClick = { menu = false; actions.openUp(mid) },
                    )
                }
            }
        }
    }
}
