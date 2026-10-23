package dev.aaa1115910.bv.ui.feature.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.Ambient
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.SectionHeader
import dev.aaa1115910.bv.ui.designsystem.component.TvBackdrop
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.focus.TvColumnPivot
import dev.aaa1115910.bv.ui.designsystem.focus.TvRowPivot
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import kotlinx.coroutines.delay

/** 电视详情的图标按钮（48dp）：点赞 / 投币 / 收藏 / 稍后再看 / UP 主（番剧为追番）。 */
@Immutable
internal data class TvDetailAction(
    val icon: ImageVector,
    val contentDescription: String,
    val checked: Boolean,
    val onClick: () -> Unit,
)

/**
 * 电视视频详情（原型 `detailTv`，CoverTheme 强制深色由调用方提供）：
 * 氛围光 + 70% 高的全幅背景；左上信息区（Eyebrow、标题、UP 主行、简介、按钮行），
 * 初始焦点在「▶ 继续 P{n} · {mm:ss}」/「▶ 播放」；下方选集 / 合集 / 相关推荐卡片行（枢轴滚动）。
 */
@Composable
internal fun VideoDetailTv(
    ui: DetailUi,
    playLabel: String,
    onPlayDefault: () -> Unit,
    actions: List<TvDetailAction>,
    currentCid: Long,
    callbacks: DetailCallbacks,
    onShowDescription: () -> Unit,
) {
    val playRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // 等第一帧布局完成再请求初始焦点
        delay(16)
        runCatching { playRequester.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        // Ambient 已经用 surface 铺底，这里不再重复画一层全屏背景
        Ambient(ui.cover, Modifier.fillMaxSize())
        TvBackdrop(ui.cover, Modifier.fillMaxSize(), heightFraction = 0.70f)

        Column(
            Modifier
                .padding(start = 48.dp, top = 30.dp)
                .width(520.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (ui.eyebrow.isNotEmpty()) {
                Text(
                    ui.eyebrow,
                    style = BvType.Eyebrow,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                ui.title,
                style = BvType.DetailTv,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (ui.seasons.size > 1) {
                TvRowPivot {
                    LazyRow(
                        modifier = Modifier
                            .focusRestorer()
                            .focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = ui.seasons,
                            key = { _, s -> s.seasonId },
                            contentType = { _, _ -> "season" },
                        ) { _, season ->
                            BvChip(
                                text = season.title,
                                selected = season.seasonId == ui.currentSeasonId,
                                onClick = {
                                    if (season.seasonId != ui.currentSeasonId) callbacks.onSeasonSelect(season.seasonId)
                                },
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ui.upName.isNotEmpty()) {
                    BvAvatar(url = ui.upFace, size = 32.dp, fallbackText = ui.upName)
                    Text(
                        ui.upName,
                        style = TvUpNameStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
                if (ui.metaTv.isNotEmpty()) {
                    Text(
                        ui.metaTv,
                        style = BvType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (ui.description.isNotBlank()) {
                Text(
                    ui.description,
                    modifier = Modifier
                        .bvFocus(scale = 1.02f, corner = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onShowDescription,
                        ),
                    style = TvDescriptionStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier
                    .padding(top = 4.dp)
                    .focusRestorer(playRequester)
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BvButton(
                    text = playLabel,
                    onClick = onPlayDefault,
                    modifier = Modifier.focusRequester(playRequester),
                    icon = Icons.Filled.PlayArrow,
                    large = true,
                )
                actions.forEach { action ->
                    BvIconButton(
                        icon = action.icon,
                        contentDescription = action.contentDescription,
                        onClick = action.onClick,
                        style = BvIconButtonStyle.Tonal,
                        size = 48.dp,
                        shape = if (action.checked) RoundedCornerShape(16.dp) else CircleShape,
                        focusCorner = if (action.checked) 16.dp else 999.dp,
                        containerColor = if (action.checked) MaterialTheme.colorScheme.primary else null,
                        contentColor = if (action.checked) MaterialTheme.colorScheme.onPrimary else null,
                    )
                }
            }
        }

        TvColumnPivot {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (ui.seasons.size > 1) 340.dp else 300.dp),
                contentPadding = PaddingValues(start = 48.dp, bottom = 27.dp),
            ) {
                ui.rows.forEach { row ->
                    item(key = "header-${row.key}") {
                        SectionHeader(title = row.title, modifier = Modifier.padding(end = 48.dp))
                    }
                    item(key = "row-${row.key}") {
                        TvRowPivot {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRestorer()
                                    .focusGroup(),
                                contentPadding = PaddingValues(start = 4.dp, end = 48.dp, top = 10.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                itemsIndexed(
                                    items = row.episodes,
                                    key = { _, ep -> ep.cid },
                                    contentType = { _, _ -> "episode" },
                                ) { _, episode ->
                                    TvEpisodeCard(
                                        episode = episode,
                                        onClick = { callbacks.onPlay(row, episode) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (ui.related.isNotEmpty()) {
                    item(key = "related-header") {
                        SectionHeader(
                            title = stringResource(R.string.video_detail_related),
                            modifier = Modifier.padding(top = 2.dp, end = 48.dp),
                        )
                    }
                    item(key = "related-row") {
                        TvRowPivot {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRestorer()
                                    .focusGroup(),
                                contentPadding = PaddingValues(start = 4.dp, end = 48.dp, top = 10.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                itemsIndexed(
                                    items = ui.related,
                                    key = { _, video -> video.avid },
                                    contentType = { _, _ -> "related" },
                                ) { _, video ->
                                    VideoCard(
                                        data = video,
                                        onClick = { callbacks.onRelatedClick(video) },
                                        width = 180.dp,
                                        showMeta = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
