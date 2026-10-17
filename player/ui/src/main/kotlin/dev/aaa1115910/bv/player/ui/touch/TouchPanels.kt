@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.touch

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.player.danmaku.DanmakuLaneDensity
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.player.ui.PlayerMenuState
import dev.aaa1115910.bv.player.ui.R
import dev.aaa1115910.bv.player.ui.design.ExpressiveSlider
import dev.aaa1115910.bv.player.ui.design.PlayerChip
import dev.aaa1115910.bv.player.ui.design.PlayerListItem
import dev.aaa1115910.bv.player.ui.design.PlayerType
import dev.aaa1115910.bv.player.ui.displayTitle
import dev.aaa1115910.bv.player.ui.formatDanmakuArea
import dev.aaa1115910.bv.player.ui.formatPercent
import dev.aaa1115910.bv.player.ui.formatSpeed
import dev.aaa1115910.bv.player.ui.indexOfCid
import kotlin.math.abs
import kotlin.math.roundToInt

/** 触摸端菜单（第 9.4 节）：画质 / 倍速 / 选集 / 弹幕 / 字幕 / 更多，一律从右侧滑出侧边面板。 */
internal enum class TouchPanel { None, Quality, Speed, Episodes, Danmaku, Subtitle, More }

private val PanelShape = RoundedCornerShape(28.dp)
private val ValueStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = PlayerType.TabularNum)
private val NameStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)

/**
 * 侧边面板外壳：at (top 16, end 16, bottom 16) width 340，圆角 28，surfaceContainer，padding 22，Column(spacedBy 20)。
 */
@Composable
internal fun TouchSidePanel(
    panel: TouchPanel,
    menuState: PlayerMenuState,
    onPlayNewVideo: (VideoListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = modifier
                .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                .width(340.dp)
                .fillMaxHeight()
                .clip(PanelShape)
                .background(MaterialTheme.colorScheme.surfaceContainer, PanelShape)
                // 面板内的空白处点击不穿透到画面手势层
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            when (panel) {
                TouchPanel.Episodes -> EpisodesPanel(menuState = menuState, onPlayNewVideo = onPlayNewVideo)
                TouchPanel.None -> {}
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (panel) {
                        TouchPanel.Quality -> QualityPanel(menuState)
                        TouchPanel.Speed -> SpeedPanel(menuState)
                        TouchPanel.Danmaku -> DanmakuPanel(menuState)
                        TouchPanel.Subtitle -> SubtitlePanel(menuState)
                        TouchPanel.More -> MorePanel(menuState)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelTitle(text: String, modifier: Modifier = Modifier) {
    Text(modifier = modifier, text = text, style = PlayerType.HeroPhone)
}

/** 小节：Eyebrow 标题 + 内容。 */
@Composable
private fun PanelSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = PlayerType.Eyebrow,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

/** 滑块：Column(spacedBy 8)：Row(SpaceBetween)(名称 13sp + 数值 13sp onSurfaceVariant 等宽) + Slider。 */
@Composable
private fun SliderBlock(
    name: String,
    value: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = name, style = NameStyle)
            Text(text = value, style = ValueStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = PlayerType.Body,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun RadioListItem(text: String, selected: Boolean, onClick: () -> Unit) {
    PlayerListItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        selected = selected,
    ) {
        Text(text = text, style = PlayerType.BodyBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ------------------------------------------------------------------ 画质（旧 DashMenu）

@Composable
private fun QualityPanel(menuState: PlayerMenuState) {
    val context = LocalContext.current
    PanelTitle(stringResource(R.string.player_ui_panel_quality))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        menuState.availableResolutions.forEach { resolution ->
            RadioListItem(
                text = resolution.getDisplayName(context),
                selected = menuState.resolution == resolution,
                onClick = { menuState.selectResolution(resolution) },
            )
        }
    }
    if (menuState.availableVideoCodecs.isNotEmpty()) {
        PanelSection(stringResource(R.string.player_ui_section_codec)) {
            ChipFlow {
                menuState.availableVideoCodecs.forEach { codec ->
                    PlayerChip(
                        text = codec.getDisplayName(context),
                        selected = menuState.videoCodec == codec,
                        onClick = { menuState.selectVideoCodec(codec) },
                    )
                }
            }
        }
    }
    if (menuState.availableAudios.isNotEmpty()) {
        PanelSection(stringResource(R.string.player_ui_section_audio)) {
            ChipFlow {
                menuState.availableAudios.forEach { audio ->
                    PlayerChip(
                        text = audio.getDisplayName(context),
                        selected = menuState.audio == audio,
                        onClick = { menuState.selectAudio(audio) },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 倍速（旧 SpeedMenu）

@Composable
private fun SpeedPanel(menuState: PlayerMenuState) {
    val context = LocalContext.current
    val range = PlayerMenuState.SpeedRange
    SliderBlock(
        name = stringResource(R.string.player_ui_panel_speed),
        value = formatSpeed(context, menuState.speed),
    ) {
        ExpressiveSlider(
            value = menuState.speed,
            onValueChange = { menuState.setSpeed((it * 100).roundToInt() / 100f) },
            valueRange = range,
            // 0.25 一档
            steps = ((range.endInclusive - range.start) / 0.25f).roundToInt() - 1,
        )
    }
    ChipFlow {
        PlayerMenuState.SpeedPresets.forEach { speed ->
            PlayerChip(
                text = formatSpeed(context, speed),
                selected = abs(menuState.speed - speed) < 0.001f,
                onClick = { menuState.setSpeed(speed) },
            )
        }
    }
}

// ------------------------------------------------------------------ 选集（旧 VideoListMenu）

@Composable
private fun EpisodesPanel(
    menuState: PlayerMenuState,
    onPlayNewVideo: (VideoListItem) -> Unit,
) {
    val context = LocalContext.current
    val list = menuState.videoList
    val inUgcSeason = list.any { it is VideoListUgcEpisode }
    val listState = rememberLazyListState()
    val currentIndex = list.indexOfCid(menuState.currentVideoCid)

    LaunchedEffect(Unit) {
        // 第 0 项是标题
        if (currentIndex >= 0) listState.scrollToItem(currentIndex + 1)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            PanelTitle(
                modifier = Modifier.padding(bottom = 18.dp),
                text = stringResource(R.string.player_ui_episodes),
            )
        }
        items(items = list) { item ->
            if (item is VideoListUgcEpisodeTitle) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
                    text = item.displayTitle(context, inUgcSeason).uppercase(),
                    style = PlayerType.Eyebrow,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                val selected = item is VideoListItemData && item.cid == menuState.currentVideoCid
                PlayerListItem(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { if (!selected) onPlayNewVideo(item) },
                    selected = selected,
                ) {
                    Text(
                        text = item.displayTitle(context, inUgcSeason),
                        style = PlayerType.BodyBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 弹幕（旧 DanmakuMenu）

private val TouchDanmakuTypes = listOf(DanmakuType.Rolling, DanmakuType.Top, DanmakuType.Bottom)

@Composable
private fun DanmakuPanel(menuState: PlayerMenuState) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PanelTitle(stringResource(R.string.player_ui_panel_danmaku))
        Switch(checked = menuState.danmakuEnabled, onCheckedChange = { menuState.setDanmakuEnabled(it) })
    }
    ChipFlow {
        TouchDanmakuTypes.forEach { type ->
            PlayerChip(
                text = stringResource(
                    when (type) {
                        DanmakuType.Top -> R.string.player_ui_danmaku_type_top
                        DanmakuType.Bottom -> R.string.player_ui_danmaku_type_bottom
                        else -> R.string.player_ui_danmaku_type_rolling
                    }
                ),
                selected = menuState.isDanmakuTypeShown(type),
                onClick = { menuState.toggleDanmakuType(type) },
            )
        }
    }
    ChipFlow {
        DanmakuLaneDensity.entries.forEach { density ->
            PlayerChip(
                text = stringResource(
                    when (density) {
                        DanmakuLaneDensity.Sparse -> R.string.player_ui_danmaku_density_sparse
                        DanmakuLaneDensity.Standard -> R.string.player_ui_danmaku_density_standard
                        DanmakuLaneDensity.Dense -> R.string.player_ui_danmaku_density_dense
                    }
                ),
                selected = menuState.danmakuLaneDensity == density,
                onClick = { menuState.setDanmakuLaneDensity(density) },
            )
        }
    }
    SliderBlock(
        name = stringResource(R.string.player_ui_danmaku_opacity),
        value = formatPercent(menuState.danmakuOpacity),
    ) {
        ExpressiveSlider(
            value = menuState.danmakuOpacity,
            onValueChange = { menuState.setDanmakuOpacity(it) },
            valueRange = 0f..1f,
        )
    }
    SliderBlock(
        name = stringResource(R.string.player_ui_danmaku_area),
        value = formatDanmakuArea(context, menuState.danmakuArea),
    ) {
        // 1/4 屏、1/2 屏、3/4 屏、全屏
        ExpressiveSlider(
            value = menuState.danmakuArea,
            onValueChange = { menuState.setDanmakuArea(it) },
            valueRange = 0.25f..1f,
            steps = 2,
        )
    }
    SliderBlock(
        name = stringResource(R.string.player_ui_danmaku_size),
        value = formatPercent(menuState.danmakuScale),
    ) {
        ExpressiveSlider(
            value = menuState.danmakuScale,
            onValueChange = { menuState.setDanmakuScale((it * 100).roundToInt() / 100f) },
            valueRange = 0.5f..2f,
        )
    }
    SwitchRow(
        text = stringResource(R.string.player_ui_danmaku_mask),
        checked = menuState.danmakuMask,
        onCheckedChange = { menuState.setDanmakuMask(it) },
    )
    SliderBlock(
        name = stringResource(R.string.player_ui_danmaku_filter_level),
        value = stringResource(R.string.player_ui_danmaku_filter_level_value, menuState.danmakuFilterLevel),
    ) {
        ExpressiveSlider(
            value = menuState.danmakuFilterLevel.toFloat(),
            onValueChange = { menuState.setDanmakuFilterLevel(it.roundToInt()) },
            valueRange = 0f..10f,
        )
    }
}

// ------------------------------------------------------------------ 字幕（来自电视 ClosedCaptionMenu）

@Composable
private fun SubtitlePanel(menuState: PlayerMenuState) {
    PanelTitle(stringResource(R.string.player_ui_panel_subtitle))
    if (menuState.subtitleTracks.isEmpty()) {
        Text(
            text = stringResource(R.string.player_ui_subtitle_empty),
            style = PlayerType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            menuState.subtitleTracks.forEach { subtitle ->
                RadioListItem(
                    text = subtitle.langDoc,
                    selected = subtitle.id == menuState.subtitleId,
                    onClick = { menuState.selectSubtitle(subtitle) },
                )
            }
        }
    }
    val fontSize = menuState.subtitleFontSize.value.roundToInt()
    SliderBlock(
        name = stringResource(R.string.player_ui_subtitle_size),
        value = stringResource(R.string.player_ui_unit_sp, fontSize),
    ) {
        ExpressiveSlider(
            value = fontSize.toFloat(),
            onValueChange = { menuState.setSubtitleFontSize(it.roundToInt().sp) },
            valueRange = 12f..48f,
        )
    }
    SliderBlock(
        name = stringResource(R.string.player_ui_subtitle_background),
        value = formatPercent(menuState.subtitleBackgroundOpacity),
    ) {
        ExpressiveSlider(
            value = menuState.subtitleBackgroundOpacity,
            onValueChange = { menuState.setSubtitleBackgroundOpacity(it) },
            valueRange = 0f..1f,
        )
    }
    val padding = menuState.subtitleBottomPadding.value.roundToInt()
    SliderBlock(
        name = stringResource(R.string.player_ui_subtitle_padding),
        value = stringResource(R.string.player_ui_unit_dp, padding),
    ) {
        ExpressiveSlider(
            value = padding.toFloat(),
            onValueChange = { menuState.setSubtitleBottomPadding(it.roundToInt().dp) },
            valueRange = 0f..48f,
        )
    }
}

// ------------------------------------------------------------------ 更多（旧 MoreMenu + 画面比例）

@Composable
private fun MorePanel(menuState: PlayerMenuState) {
    val context = LocalContext.current
    PanelTitle(stringResource(R.string.player_ui_panel_more))
    PanelSection(stringResource(R.string.player_ui_section_play_mode)) {
        ChipFlow {
            PlayMode.entries.forEach { mode ->
                PlayerChip(
                    text = mode.getDisplayName(context),
                    selected = menuState.playMode == mode,
                    onClick = { menuState.setPlayMode(mode) },
                )
            }
        }
    }
    PanelSection(stringResource(R.string.player_ui_section_aspect_ratio)) {
        ChipFlow {
            VideoAspectRatio.entries.forEach { ratio ->
                PlayerChip(
                    text = ratio.getDisplayName(context),
                    selected = menuState.aspectRatio == ratio,
                    onClick = { menuState.setAspectRatio(ratio) },
                )
            }
        }
    }
}
