@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.touch

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bv.player.ui.R
import dev.aaa1115910.bv.player.ui.SeekPreviewRow
import dev.aaa1115910.bv.player.ui.design.CinemaScrim
import dev.aaa1115910.bv.player.ui.design.CompactChipPadding
import dev.aaa1115910.bv.player.ui.design.CompactChipText
import dev.aaa1115910.bv.player.ui.design.DraggableWavyProgress
import dev.aaa1115910.bv.player.ui.design.LocalPlayerDpad
import dev.aaa1115910.bv.player.ui.design.PlainIconButton
import dev.aaa1115910.bv.player.ui.design.PlayerChip
import dev.aaa1115910.bv.player.ui.design.PlayerType
import dev.aaa1115910.bv.player.ui.design.animatedPillShape
import dev.aaa1115910.bv.player.ui.design.bvFocus
import dev.aaa1115910.bv.util.formatHourMinSec

/** 控制栏上吞掉点击，避免点到按钮之间的空白处时切换控制层（旧版 noRippleClickable {} 的作用）。 */
private fun Modifier.consumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

private val TabularTime = TextStyle(fontFeatureSettings = PlayerType.TabularNum)

/** 手机 / 平板 两套尺寸（原型 playerPhone / playerTablet）。 */
private class FullscreenMetrics(
    val bottomStart: Dp,
    val bottomEnd: Dp,
    val bottomPadding: Dp,
    val maxBottomWidth: Dp,
    val columnSpacing: Dp,
    val rowSpacing: Dp,
    val fontSize: Float,
)

private val PhoneMetrics = FullscreenMetrics(
    bottomStart = 20.dp, bottomEnd = 20.dp, bottomPadding = 12.dp, maxBottomWidth = Dp.Unspecified,
    columnSpacing = 6.dp, rowSpacing = 12.dp, fontSize = 12f,
)
private val TabletMetrics = FullscreenMetrics(
    bottomStart = 28.dp, bottomEnd = 28.dp, bottomPadding = 20.dp, maxBottomWidth = 800.dp,
    columnSpacing = 8.dp, rowSpacing = 14.dp, fontSize = 13f,
)

/** 预览缩略图与进度条顶部的间距（原型：缩略图底边在 bottom 78 处，进度条顶部在 bottom 54 处）。 */
private val PreviewGap = 24.dp

/**
 * 全屏控制层（第 9.4 节 手机 780×360 / 平板）：
 * CinemaScrim + 顶栏 + 中央 72dp 播放按钮 + 底部（可拖动 WavyProgress + 按钮行）。
 * 侧边面板打开时隐藏顶栏和中央按钮（原型 playerTablet）。
 */
@Composable
internal fun FullscreenControls(
    isTablet: Boolean,
    panelOpen: Boolean,
    title: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    danmakuEnabled: Boolean,
    showEpisodes: Boolean,
    hasNext: Boolean,
    speedText: String,
    resolutionText: String,
    videoShot: VideoShot?,
    dragFraction: Float?,
    onDragFractionChange: (Float?) -> Unit,
    onBack: () -> Unit,
    onOpenSubtitle: () -> Unit,
    onToggleDanmaku: () -> Unit,
    onOpenMore: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenQuality: () -> Unit,
    onExitFullScreen: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = if (isTablet) TabletMetrics else PhoneMetrics
    val safeDuration = duration.coerceAtLeast(1L)
    val shownPosition = dragFraction?.let { (it * safeDuration).toLong() } ?: position

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(modifier = modifier.fillMaxSize()) {
            CinemaScrim()

            if (!panelOpen) {
                // 顶栏 at (top 12, start 16, end 16)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                        .consumeTaps(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlainIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.player_ui_cd_back),
                        onClick = onBack,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    PlainIconButton(
                        icon = Icons.Filled.Subtitles,
                        contentDescription = stringResource(R.string.player_ui_cd_subtitle),
                        onClick = onOpenSubtitle,
                    )
                    PlayerChip(
                        text = stringResource(
                            if (danmakuEnabled) R.string.player_ui_danmaku_on else R.string.player_ui_danmaku_off
                        ),
                        selected = danmakuEnabled,
                        onClick = onToggleDanmaku,
                        contentPadding = CompactChipPadding,
                        textStyle = CompactChipText.copy(fontSize = 12.sp),
                        unselectedContentColor = Color.White,
                        unselectedBorderColor = Color.White.copy(alpha = 0.4f),
                    )
                    PlainIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.player_ui_cd_more),
                        onClick = onOpenMore,
                    )
                }

                // 中央大按钮：中心比画面中心高 10dp（原型 margin-top -46）
                CenterPlayButton(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-10).dp),
                    isPlaying = isPlaying,
                    onClick = onPlayPause,
                )
            }

            // 底部 Column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = metrics.bottomStart, end = metrics.bottomEnd, bottom = metrics.bottomPadding)
                    .widthIn(max = metrics.maxBottomWidth)
                    .fillMaxWidth()
                    .consumeTaps(),
                verticalArrangement = Arrangement.spacedBy(metrics.columnSpacing),
            ) {
                SeekPreviewRow(
                    bottomGap = PreviewGap - metrics.columnSpacing,
                    visible = dragFraction != null,
                    fraction = dragFraction ?: 0f,
                    videoShot = videoShot,
                    position = shownPosition,
                )
                DraggableWavyProgress(
                    progress = position / safeDuration.toFloat(),
                    waving = isPlaying,
                    onDrag = onDragFractionChange,
                    onSeek = { onSeek((it * safeDuration).toLong()) },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.rowSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlainIconButton(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.player_ui_cd_pause else R.string.player_ui_cd_play
                        ),
                        onClick = onPlayPause,
                    )
                    PlainIconButton(
                        icon = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.player_ui_cd_next),
                        onClick = onNext,
                        enabled = hasNext,
                    )
                    Text(
                        text = stringResource(
                            R.string.player_ui_time_progress,
                            shownPosition.formatHourMinSec(),
                            duration.formatHourMinSec()
                        ),
                        style = TabularTime.copy(fontSize = metrics.fontSize.sp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (showEpisodes) {
                        VideoOutlineChip(text = stringResource(R.string.player_ui_episodes), onClick = onOpenEpisodes)
                    }
                    VideoOutlineChip(text = speedText, onClick = onOpenSpeed)
                    PlayerChip(
                        text = resolutionText,
                        selected = true,
                        onClick = onOpenQuality,
                        contentPadding = CompactChipPadding,
                        textStyle = CompactChipText,
                    )
                    PlainIconButton(
                        icon = Icons.Filled.FullscreenExit,
                        contentDescription = stringResource(R.string.player_ui_cd_exit_fullscreen),
                        onClick = onExitFullScreen,
                    )
                }
            }
        }
    }
}

/** 画面上的描边小标签：白字 + 白 α0.4 描边（原型「选集」「1.0×」）。 */
@Composable
private fun VideoOutlineChip(text: String, onClick: () -> Unit) {
    PlayerChip(
        text = text,
        selected = false,
        onClick = onClick,
        contentPadding = CompactChipPadding,
        textStyle = CompactChipText,
        unselectedContentColor = Color.White,
        unselectedBorderColor = Color.White.copy(alpha = 0.4f),
    )
}

/**
 * 中央大按钮 72dp：暂停中 = 圆角 24 的方形、图标播放；播放中 = 圆形、图标暂停。
 * 背景 primaryContainer，形状变化 fastSpatialSpec。
 */
@Composable
private fun CenterPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dpad = LocalPlayerDpad.current
    val shape = animatedPillShape(from = 24.dp, pill = isPlaying, fast = true, label = "centerPlayShape")
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(72.dp)
            .bvFocus(dpad = dpad, corner = 36.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (isPlaying) R.string.player_ui_cd_pause else R.string.player_ui_cd_play),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * 详情页内嵌播放器的精简控制层（第 9.3 节）：
 * CinemaScrim；左上返回、右上更多；底部 Row：暂停 | WavyProgress(inversePrimary) | 时间 | 全屏。
 * 手机：start/end 12, bottom 8, spacedBy 10，图标 20dp，时间「06:12」11sp；
 * 平板：start/end 20, bottom 14, spacedBy 12，时间「06:12 / 18:42」12sp。
 */
@Composable
internal fun EmbeddedControls(
    isTablet: Boolean,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onBack: () -> Unit,
    onMore: (() -> Unit)?,
    onPlayPause: () -> Unit,
    onEnterFullScreen: () -> Unit,
    onSeek: (Long) -> Unit,
    onDragFractionChange: (Float?) -> Unit,
    dragFraction: Float?,
    modifier: Modifier = Modifier,
) {
    val safeDuration = duration.coerceAtLeast(1L)
    val shownPosition = dragFraction?.let { (it * safeDuration).toLong() } ?: position
    val iconSize = if (isTablet) 24.dp else 20.dp

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(modifier = modifier.fillMaxSize()) {
            CinemaScrim()
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 2.dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                PlainIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.player_ui_cd_back),
                    onClick = onBack,
                )
            }
            if (onMore != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 2.dp)
                        .size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlainIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.player_ui_cd_more),
                        onClick = onMore,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = if (isTablet) 20.dp else 12.dp,
                        end = if (isTablet) 20.dp else 12.dp,
                        bottom = if (isTablet) 14.dp else 8.dp,
                    )
                    .consumeTaps(),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainIconButton(
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.player_ui_cd_pause else R.string.player_ui_cd_play
                    ),
                    onClick = onPlayPause,
                    size = iconSize,
                )
                DraggableWavyProgress(
                    modifier = Modifier.weight(1f),
                    progress = position / safeDuration.toFloat(),
                    waving = isPlaying,
                    color = MaterialTheme.colorScheme.inversePrimary,
                    onDrag = onDragFractionChange,
                    onSeek = { onSeek((it * safeDuration).toLong()) },
                )
                Text(
                    text = if (isTablet) {
                        stringResource(
                            R.string.player_ui_time_progress,
                            shownPosition.formatHourMinSec(),
                            duration.formatHourMinSec()
                        )
                    } else {
                        shownPosition.formatHourMinSec()
                    },
                    style = TabularTime.copy(fontSize = if (isTablet) 12.sp else 11.sp),
                )
                PlainIconButton(
                    icon = Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.player_ui_cd_fullscreen),
                    onClick = onEnterFullScreen,
                    size = iconSize,
                )
            }
        }
    }
}
