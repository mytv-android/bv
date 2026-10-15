@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
)

package dev.aaa1115910.bv.player.ui.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisode
import dev.aaa1115910.bv.player.entity.VideoListUgcEpisodeTitle
import dev.aaa1115910.bv.player.ui.PlayerMenuState
import dev.aaa1115910.bv.player.ui.R
import dev.aaa1115910.bv.player.ui.design.PlayerListItem
import dev.aaa1115910.bv.player.ui.design.PlayerType
import dev.aaa1115910.bv.player.ui.displayTitle
import dev.aaa1115910.bv.player.ui.formatDanmakuArea
import dev.aaa1115910.bv.player.ui.formatPercent
import dev.aaa1115910.bv.player.ui.formatSpeed
import dev.aaa1115910.bv.player.ui.indexOfCid
import dev.aaa1115910.bv.util.requestFocus
import kotlin.math.abs

/** 电视菜单的四个分类（第 9.4 节）：画面 / 弹幕 / 字幕 / 其他。 */
internal enum class TvMenuCategory(val labelRes: Int) {
    Picture(R.string.player_ui_menu_picture),
    Danmaku(R.string.player_ui_menu_danmaku),
    Subtitle(R.string.player_ui_menu_subtitle),
    Others(R.string.player_ui_menu_others),
}

/** 面板宽 300（第 9.4 节）。 */
internal val TvPanelWidth = 300.dp

private val PanelShape = RoundedCornerShape(28.dp)
private val CategoryText = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
private val OptionText = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)

// 电视菜单里「无级调节」的项改为常用值单选（与原型一致：每项都是带 ✓ 的选项）
private val DanmakuScalePresets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f)
private val DanmakuOpacityPresets = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1f)
private val DanmakuAreaPresets = listOf(0.25f, 0.5f, 0.75f, 1f)
private val SubtitleSizePresets = listOf(16, 20, 24, 28, 32, 40, 48)
private val SubtitleBackgroundPresets = listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
private val SubtitlePaddingPresets = listOf(0, 8, 12, 16, 24, 32, 48)

private fun Float.near(other: Float) = abs(this - other) < 0.001f

/** 菜单面板外壳：at (top 16, end 16, bottom 16) width 300，圆角 28，surfaceContainer。 */
@Composable
private fun TvPanelShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = modifier
                .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                .width(TvPanelWidth)
                .fillMaxHeight()
                .clip(PanelShape)
                .background(MaterialTheme.colorScheme.surfaceContainer, PanelShape)
        ) {
            content()
        }
    }
}

@Composable
private fun Eyebrow(text: String, top: Dp) {
    Text(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = top, bottom = 6.dp),
        text = text.uppercase(),
        style = PlayerType.Eyebrow,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
    )
}

/** 选项行：BvListItem（padding 8×12），选中显示 ✓ 并 secondaryContainer。 */
@Composable
private fun OptionItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerListItem(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        selected = selected,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(24.dp))
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Text(text = text, style = OptionText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 一组带 Eyebrow 标题的单选项。 */
private fun <T> LazyListScope.optionSection(
    key: String,
    title: @Composable () -> String,
    first: Boolean,
    options: List<T>,
    label: @Composable (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    item(key = "$key-title") { Eyebrow(title(), if (first) 0.dp else 10.dp) }
    itemsIndexed(options, key = { index, _ -> "$key-$index" }) { _, option ->
        OptionItem(text = label(option), selected = isSelected(option), onClick = { onSelect(option) })
    }
}

/**
 * 电视菜单面板（长按确认或菜单键打开）：
 * 左列分类（宽 96，surfaceContainerHigh，聚焦即选中），右列选项（Eyebrow 标题 + ✓ 选项）。
 * 菜单内容沿用旧 MenuController：画面（分辨率、编码、画面比例、倍速、音质）、
 * 弹幕（开关、类型、大小、透明度、区域、蒙版）、字幕（轨道、字号、背景透明度、底部边距）、其他（播放模式）。
 */
@Composable
internal fun TvMenuPanel(
    menuState: PlayerMenuState,
    category: TvMenuCategory,
    onCategoryChange: (TvMenuCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val categoryRequesters = remember { TvMenuCategory.entries.associateWith { FocusRequester() } }

    LaunchedEffect(Unit) {
        categoryRequesters.getValue(category).requestFocus(scope)
    }

    TvPanelShell(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .focusRestorer(categoryRequesters.getValue(category))
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TvMenuCategory.entries.forEach { item ->
                    PlayerListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(categoryRequesters.getValue(item)),
                        onClick = { onCategoryChange(item) },
                        selected = item == category,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        onFocused = { if (it) onCategoryChange(item) },
                    ) {
                        Text(
                            text = stringResource(item.labelRes),
                            style = CategoryText,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRestorer()
                    .focusGroup(),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                when (category) {
                    TvMenuCategory.Picture -> pictureOptions(menuState, context)
                    TvMenuCategory.Danmaku -> danmakuOptions(menuState, context)
                    TvMenuCategory.Subtitle -> subtitleOptions(menuState)
                    TvMenuCategory.Others -> othersOptions(menuState, context)
                }
            }
        }
    }
}

private fun LazyListScope.pictureOptions(menuState: PlayerMenuState, context: android.content.Context) {
    optionSection(
        key = "resolution",
        title = { stringResource(R.string.player_ui_section_resolution) },
        first = true,
        options = menuState.availableResolutions,
        label = { it.getDisplayName(context) },
        isSelected = { it == menuState.resolution },
        onSelect = { menuState.selectResolution(it) },
    )
    optionSection(
        key = "codec",
        title = { stringResource(R.string.player_ui_section_codec) },
        first = false,
        options = menuState.availableVideoCodecs,
        label = { it.getDisplayName(context) },
        isSelected = { it == menuState.videoCodec },
        onSelect = { menuState.selectVideoCodec(it) },
    )
    optionSection(
        key = "aspect",
        title = { stringResource(R.string.player_ui_section_aspect_ratio) },
        first = false,
        options = VideoAspectRatio.entries,
        label = { it.getDisplayName(context) },
        isSelected = { it == menuState.aspectRatio },
        onSelect = { menuState.setAspectRatio(it) },
    )
    optionSection(
        key = "speed",
        title = { stringResource(R.string.player_ui_section_speed) },
        first = false,
        options = PlayerMenuState.SpeedPresets,
        label = { formatSpeed(context, it) },
        isSelected = { it.near(menuState.speed) },
        onSelect = { menuState.setSpeed(it) },
    )
    optionSection(
        key = "audio",
        title = { stringResource(R.string.player_ui_section_audio) },
        first = false,
        options = menuState.availableAudios,
        label = { it.getDisplayName(context) },
        isSelected = { it == menuState.audio },
        onSelect = { menuState.selectAudio(it) },
    )
}

private fun LazyListScope.danmakuOptions(menuState: PlayerMenuState, context: android.content.Context) {
    optionSection(
        key = "danmakuSwitch",
        title = { stringResource(R.string.player_ui_section_danmaku_switch) },
        first = true,
        options = listOf(true, false),
        label = { stringResource(if (it) R.string.player_ui_on else R.string.player_ui_off) },
        isSelected = { it == menuState.danmakuEnabled },
        onSelect = { menuState.setDanmakuEnabled(it) },
    )
    optionSection(
        key = "danmakuType",
        title = { stringResource(R.string.player_ui_section_danmaku_type) },
        first = false,
        options = DanmakuType.entries,
        label = { it.getDisplayName(context) },
        isSelected = { menuState.danmakuTypes.contains(it) },
        onSelect = { menuState.toggleDanmakuType(it) },
    )
    optionSection(
        key = "danmakuScale",
        title = { stringResource(R.string.player_ui_danmaku_size) },
        first = false,
        options = DanmakuScalePresets,
        label = { formatPercent(it) },
        isSelected = { it.near(menuState.danmakuScale) },
        onSelect = { menuState.setDanmakuScale(it) },
    )
    optionSection(
        key = "danmakuOpacity",
        title = { stringResource(R.string.player_ui_danmaku_opacity) },
        first = false,
        options = DanmakuOpacityPresets,
        label = { formatPercent(it) },
        isSelected = { it.near(menuState.danmakuOpacity) },
        onSelect = { menuState.setDanmakuOpacity(it) },
    )
    optionSection(
        key = "danmakuArea",
        title = { stringResource(R.string.player_ui_danmaku_area) },
        first = false,
        options = DanmakuAreaPresets,
        label = { formatDanmakuArea(context, it) },
        isSelected = { it.near(menuState.danmakuArea) },
        onSelect = { menuState.setDanmakuArea(it) },
    )
    optionSection(
        key = "danmakuMask",
        title = { stringResource(R.string.player_ui_section_danmaku_mask) },
        first = false,
        options = listOf(true, false),
        label = { stringResource(if (it) R.string.player_ui_on else R.string.player_ui_off) },
        isSelected = { it == menuState.danmakuMask },
        onSelect = { menuState.setDanmakuMask(it) },
    )
}

private fun LazyListScope.subtitleOptions(menuState: PlayerMenuState) {
    optionSection(
        key = "subtitleTrack",
        title = { stringResource(R.string.player_ui_section_subtitle_track) },
        first = true,
        options = menuState.subtitleTracks,
        label = { it.langDoc },
        isSelected = { it.id == menuState.subtitleId },
        onSelect = { menuState.selectSubtitle(it) },
    )
    optionSection(
        key = "subtitleSize",
        title = { stringResource(R.string.player_ui_subtitle_size) },
        first = false,
        options = SubtitleSizePresets,
        label = { stringResource(R.string.player_ui_unit_sp, it) },
        isSelected = { it.toFloat().near(menuState.subtitleFontSize.value) },
        onSelect = { menuState.setSubtitleFontSize(it.sp) },
    )
    optionSection(
        key = "subtitleBackground",
        title = { stringResource(R.string.player_ui_subtitle_background) },
        first = false,
        options = SubtitleBackgroundPresets,
        label = { formatPercent(it) },
        isSelected = { it.near(menuState.subtitleBackgroundOpacity) },
        onSelect = { menuState.setSubtitleBackgroundOpacity(it) },
    )
    optionSection(
        key = "subtitlePadding",
        title = { stringResource(R.string.player_ui_subtitle_padding) },
        first = false,
        options = SubtitlePaddingPresets,
        label = { stringResource(R.string.player_ui_unit_dp, it) },
        isSelected = { it.toFloat().near(menuState.subtitleBottomPadding.value) },
        onSelect = { menuState.setSubtitleBottomPadding(it.dp) },
    )
}

private fun LazyListScope.othersOptions(menuState: PlayerMenuState, context: android.content.Context) {
    optionSection(
        key = "playMode",
        title = { stringResource(R.string.player_ui_section_play_mode) },
        first = true,
        options = PlayMode.entries,
        label = { it.getDisplayName(context) },
        isSelected = { it == menuState.playMode },
        onSelect = { menuState.setPlayMode(it) },
    )
}

/**
 * 选集面板（↑ 键或控制栏「选集」按钮打开），由旧 VideoListController 改写，外壳与菜单面板相同。
 * 打开时焦点落在当前播放的那一项。
 */
@Composable
internal fun TvVideoListPanel(
    menuState: PlayerMenuState,
    onPlayNewVideo: (VideoListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val list = menuState.videoList
    val inUgcSeason = list.any { it is VideoListUgcEpisode }
    val listState = rememberLazyListState()
    val currentFocusRequester = remember { FocusRequester() }
    val currentIndex = list.indexOfCid(menuState.currentVideoCid)

    LaunchedEffect(Unit) {
        // 第 0 项是标题
        if (currentIndex >= 0) listState.scrollToItem(currentIndex + 1)
        currentFocusRequester.requestFocus(scope)
    }

    TvPanelShell(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusRestorer(currentFocusRequester)
                .focusGroup(),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "title") { Eyebrow(stringResource(R.string.player_ui_episodes), 0.dp) }
            itemsIndexed(list) { index, item ->
                if (item is VideoListUgcEpisodeTitle) {
                    Eyebrow(item.displayTitle(context, inUgcSeason), 10.dp)
                } else {
                    val selected = item is VideoListItemData && item.cid == menuState.currentVideoCid
                    val requesterModifier = if (index == currentIndex || (currentIndex < 0 && index == 0)) {
                        Modifier.focusRequester(currentFocusRequester)
                    } else {
                        Modifier
                    }
                    OptionItem(
                        modifier = requesterModifier,
                        text = item.displayTitle(context, inUgcSeason),
                        selected = selected,
                        onClick = { if (!selected) onPlayNewVideo(item) },
                    )
                }
            }
        }
    }
}
