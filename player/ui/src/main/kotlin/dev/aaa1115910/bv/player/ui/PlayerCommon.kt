@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListInteractiveNode
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.player.ui.design.PlayerType
import dev.aaa1115910.bv.player.util.getImage
import dev.aaa1115910.bv.util.formatHourMinSec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// 文案工具
// ---------------------------------------------------------------------------------------------

/** 选集列表里的显示文字，规则照搬旧 VideoListMenu / VideoListController。 */
internal fun VideoListItem.displayTitle(context: Context, inUgcSeason: Boolean): String = when (this) {
    is VideoListPart -> (if (inUgcSeason) " - " else "") +
            context.getString(R.string.player_ui_part_label, index + 1, title)

    is VideoListInteractiveNode -> context.getString(
        R.string.player_ui_branch_label,
        index + 1,
        partTitle.ifBlank { context.getString(R.string.player_ui_branch_unnamed) }
    )
    is VideoListUgcEpisode -> context.getString(R.string.player_ui_episode_label, index + 1, title)
    is VideoListPgcEpisode -> title
    is VideoListUgcEpisodeTitle -> context.getString(R.string.player_ui_episode_label, index + 1, title)
    else -> ""
}

internal fun List<VideoListItem>.indexOfCid(cid: Long): Int =
    indexOfFirst { it is VideoListItemData && it.cid == cid }

/** 当前视频之后的第一个可播放条目；没有则为 null。 */
internal fun List<VideoListItem>.nextAfter(cid: Long): VideoListItemData? {
    val index = indexOfCid(cid)
    if (index < 0) return null
    return drop(index + 1).firstOrNull { it is VideoListItemData } as VideoListItemData?
}

/** 倍速文字：1.0×、1.25×。 */
internal fun formatSpeed(context: Context, speed: Float): String {
    val rounded = (speed * 100).roundToInt() / 100f
    val number = if (abs(rounded * 10 - (rounded * 10).roundToInt()) < 0.001f) {
        String.format(Locale.ROOT, "%.1f", rounded)
    } else {
        String.format(Locale.ROOT, "%.2f", rounded)
    }
    return context.getString(R.string.player_ui_speed_value, number)
}

internal fun formatPercent(value: Float): String =
    NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }.format(value)

/** 弹幕显示区域文字：1/4 屏、1/2 屏、3/4 屏、全屏，其余显示百分比。 */
internal fun formatDanmakuArea(context: Context, area: Float): String = when {
    area >= 0.99f -> context.getString(R.string.player_ui_area_full)
    abs(area - 0.75f) < 0.01f -> context.getString(R.string.player_ui_area_fraction, "3/4")
    abs(area - 0.5f) < 0.01f -> context.getString(R.string.player_ui_area_fraction, "1/2")
    abs(area - 0.25f) < 0.01f -> context.getString(R.string.player_ui_area_fraction, "1/4")
    else -> formatPercent(area)
}

/** 当前视频的分 P 标签：「P{n} {分P名}」/「EP{n} {标题}」/ 番剧直接用分集标题。 */
@Composable
internal fun rememberPartLabel(): String? {
    val context = LocalContext.current
    val config = LocalVideoPlayerConfigData.current
    val info = LocalVideoPlayerVideoInfoData.current
    val list = config.availableVideoList
    if (list.count { it is VideoListItemData } <= 1) return null
    return when (val current = list.getOrNull(list.indexOfCid(config.currentVideoCid))) {
        is VideoListPart -> context.getString(R.string.player_ui_part_label, current.index + 1, info.partTitle)
        is VideoListUgcEpisode -> context.getString(R.string.player_ui_episode_label, current.index + 1, info.partTitle)
        else -> info.partTitle.takeIf { it.isNotBlank() }
    }
}

// ---------------------------------------------------------------------------------------------
// 字幕
// ---------------------------------------------------------------------------------------------

/** 底部字幕，照搬旧电视版 BottomSubtitle；触摸端也用它（第 11 节第 6 条）。 */
@Composable
internal fun BottomSubtitle(modifier: Modifier = Modifier) {
    val config = LocalVideoPlayerConfigData.current
    val seekData = LocalVideoPlayerSeekData.current
    val subtitleData = config.currentSubtitleData
    val time = seekData.position

    var currentText by remember { mutableStateOf("") }

    LaunchedEffect(time, subtitleData) {
        runCatching {
            currentText = subtitleData.find { it.isShowing(time) }?.content ?: ""
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (currentText != "") {
            Text(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = config.currentSubtitleBottomPadding)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.Black.copy(alpha = config.currentSubtitleBackgroundOpacity))
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                text = currentText,
                color = Color.White,
                fontSize = config.currentSubtitleFontSize,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 拖动 / 快进时的预览缩略图（第 9.4 节）
// ---------------------------------------------------------------------------------------------

private val PreviewShape = RoundedCornerShape(14.dp)
private val PreviewTimeStyle = TextStyle(
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = PlayerType.TabularNum,
)

/** 预览缩略图宽度 140dp，比例 16:10。 */
internal val SeekPreviewWidth = 140.dp
internal val SeekPreviewHeight = SeekPreviewWidth * 10 / 16

/**
 * 预览缩略图：140 宽 16:10 圆角 14，白色 2dp 描边，图内底部居中时间 11sp SemiBold。
 * 图像沿用旧电视版 VideoShot 的取帧逻辑（videoshot 雪碧图），解码放到后台线程。
 */
@Composable
internal fun SeekPreviewThumbnail(
    videoShot: VideoShot?,
    position: Long,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val second = (position / 1000).toInt()

    LaunchedEffect(videoShot, second) {
        if (videoShot == null || view.isInEditMode) return@LaunchedEffect
        val image = withContext(Dispatchers.Default) {
            runCatching { videoShot.getImage(second).asImageBitmap() }.getOrNull()
        }
        if (image != null) bitmap = image
    }

    Box(
        modifier = modifier
            .width(SeekPreviewWidth)
            .aspectRatio(16f / 10f)
            .border(2.dp, Color.White, PreviewShape)
            .clip(PreviewShape)
            .background(Color.Black),
    ) {
        bitmap?.let {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            text = position.formatHourMinSec(),
            style = PreviewTimeStyle,
            color = Color.White,
        )
    }
}

/**
 * 一行宽度的容器：在 [fraction] 对应的横向位置正上方放预览缩略图（左右不超出本行），
 * 缩略图下方再留 [bottomGap]。不显示时高度为 0。
 */
@Composable
internal fun SeekPreviewRow(
    visible: Boolean,
    fraction: Float,
    videoShot: VideoShot?,
    position: Long,
    modifier: Modifier = Modifier,
    bottomGap: Dp = 0.dp,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomGap)
        ) {
            val maxOffset = (maxWidth - SeekPreviewWidth).coerceAtLeast(0.dp)
            val x = (maxWidth * fraction.coerceIn(0f, 1f) - SeekPreviewWidth / 2).coerceIn(0.dp, maxOffset)
            SeekPreviewThumbnail(
                modifier = Modifier.offset(x = x),
                videoShot = videoShot,
                position = position,
            )
        }
    }
}
