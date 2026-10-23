package dev.aaa1115910.bv.ui.feature.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.ActionItem
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.CardFrame
import dev.aaa1115910.bv.ui.designsystem.component.DurationBadge
import dev.aaa1115910.bv.ui.designsystem.icon.BvIcons
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.viewmodel.video.VideoActionsViewModel

/** 详情页弹出层的开关（投币、收藏夹、分 P 抽屉、电视简介、内嵌播放器「更多」）。 */
@Stable
internal class DetailDialogs {
    var coin by mutableStateOf(false)
    var favorite by mutableStateOf(false)

    /** 正在显示的分 P 抽屉对应的行（[DetailEpisodeRow.key]），null 表示不显示 */
    var episodesRow by mutableStateOf<String?>(null)
    var description by mutableStateOf(false)
    var more by mutableStateOf(false)
}

/** 收藏按钮：照抄旧电视版 FavoriteButton——未收藏时直接加入默认收藏夹，已收藏时弹出收藏夹多选对话框。 */
internal fun onFavoriteClick(vm: VideoActionsViewModel, dialogs: DetailDialogs) {
    if (vm.favoured && vm.isLoggedIn) {
        dialogs.favorite = true
        vm.loadFavoriteFolders()
    } else {
        vm.addToDefaultFavoriteFolder()
    }
}

/** 投币按钮：已投满 2 枚时不再弹出对话框。 */
internal fun onCoinClick(vm: VideoActionsViewModel, dialogs: DetailDialogs) {
    if (!vm.isLoggedIn) {
        vm.coin(1, false) // 未登录：由 ViewModel 发出「请先登录」
        return
    }
    if (vm.coinCount < 2) dialogs.coin = true
}

/**
 * 视频详情的 5 个连接按钮（第 7.6 节）：点赞 / 投币 / 收藏显示数量，稍后 / 分享显示文字。
 * [likes] / [coins] / [favorites] 为视频详情里的统计数，番剧传 null（只显示文字）。
 */
@Composable
internal fun rememberDetailActionItems(
    vm: VideoActionsViewModel,
    likes: Int?,
    coins: Int?,
    favorites: Int?,
    dialogs: DetailDialogs,
    onShare: () -> Unit,
): List<ActionItem> {
    val context = LocalContext.current
    val likeLabel = likes?.let { context.formatCount(it + vm.likeDelta) } ?: stringResource(R.string.video_detail_like)
    val coinLabel = coins?.let { context.formatCount(it + vm.coinDelta) } ?: stringResource(R.string.video_detail_coin)
    val favoriteLabel = favorites?.let { context.formatCount(it + vm.favoriteDelta) }
        ?: stringResource(R.string.video_detail_favorite)
    val laterLabel = stringResource(R.string.video_detail_later)
    val shareLabel = stringResource(R.string.video_detail_share)
    return listOf(
        ActionItem(
            icon = if (vm.liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            label = likeLabel,
            checked = vm.liked,
            onClick = { vm.toggleLike() },
        ),
        ActionItem(
            icon = BvIcons.Coin,
            label = coinLabel,
            checked = vm.coinCount > 0,
            onClick = { onCoinClick(vm, dialogs) },
        ),
        ActionItem(
            icon = if (vm.favoured) Icons.Filled.Star else Icons.Outlined.StarBorder,
            label = favoriteLabel,
            checked = vm.favoured,
            onClick = { onFavoriteClick(vm, dialogs) },
        ),
        ActionItem(
            icon = if (vm.inToView) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
            label = laterLabel,
            checked = vm.inToView,
            onClick = { vm.toggleToView() },
        ),
        ActionItem(
            icon = Icons.Outlined.Share,
            label = shareLabel,
            checked = false,
            onClick = onShare,
        ),
    )
}

/** 「+ 关注」按钮：高 36、水平内边距 16；已关注为 Tonal「已关注」（照抄旧电视 UpButton 的切换逻辑）。 */
@Composable
internal fun DetailFollowButton(follow: DetailFollow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val followed = follow.followed == true
    BvButton(
        text = if (followed) follow.followedLabel else follow.label,
        onClick = onClick,
        modifier = modifier,
        style = if (followed) BvButtonStyle.Tonal else BvButtonStyle.Filled,
        icon = if (followed) Icons.Rounded.Check else Icons.Rounded.Add,
        height = 36.dp,
        horizontalPadding = 16.dp,
    )
}

/** UP 主行（手机）：头像 40 + 名字 14sp Bold / 副信息 + 关注按钮。 */
@Composable
internal fun DetailUpRow(
    ui: DetailUi,
    follow: DetailFollow?,
    onUpClick: () -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
    meta: String = ui.upMeta,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvAvatar(
            url = ui.upFace,
            size = avatarSize,
            fallbackText = ui.upName,
            modifier = Modifier.clickable(onClick = onUpClick),
        )
        Column(
            Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onUpClick,
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                ui.upName,
                style = BvType.Body.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        if (follow != null && follow.followed != null) {
            DetailFollowButton(follow, onFollowClick)
        }
    }
}

/** 标题（点击展开全部标题 + 简介 + 标签）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailTitleBlock(
    ui: DetailUi,
    titleStyle: androidx.compose.ui.text.TextStyle,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 2,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            ui.title,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { expanded = !expanded },
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded) {
            if (ui.description.isNotBlank()) {
                Text(
                    ui.description,
                    style = BvType.Small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ui.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ui.tags.forEach { tag ->
                        BvChip(
                            text = tag.name,
                            selected = false,
                            onClick = { onTagClick(tag) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            textStyle = BvType.Label,
                        )
                    }
                }
            }
        }
    }
}

/** 分 P / 剧集标签行（`.m-chips`，间距 8），当前播放的选中，并自动滚到当前项。 */
@Composable
internal fun DetailEpisodeChips(
    row: DetailEpisodeRow,
    currentCid: Long,
    onPlay: (DetailEpisode) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    val state = rememberLazyListState()
    val currentIndex = remember(row.episodes, currentCid) {
        row.episodes.indexOfFirst { it.cid == currentCid }
    }
    LaunchedEffect(currentIndex) {
        if (currentIndex > 0) runCatching { state.animateScrollToItem((currentIndex - 1).coerceAtLeast(0)) }
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = row.episodes,
            key = { _, ep -> ep.cid },
            contentType = { _, _ -> "episode" },
        ) { _, ep ->
            BvChip(
                text = ep.label,
                selected = ep.cid == currentCid,
                onClick = { onPlay(ep) },
            )
        }
    }
}

/** 季度选择行（番剧多季时在标题下方）。 */
@Composable
internal fun DetailSeasonChips(
    ui: DetailUi,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    if (ui.seasons.size <= 1) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
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
                onClick = { if (season.seasonId != ui.currentSeasonId) onSelect(season.seasonId) },
            )
        }
    }
}

/**
 * 电视分 P / 剧集卡片（第 9.3 节）：宽 170，封面 16:10 + 时长角标 + 已看进度条，标题 1 行。
 * 用 CardFrame 获得电视卡片的焦点效果。
 */
@Composable
internal fun TvEpisodeCard(
    episode: DetailEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardFrame(
        cover = episode.cover,
        aspectRatio = 16f / 10f,
        onClick = onClick,
        modifier = modifier,
        width = 170.dp,
        coverContent = {
            if (episode.durationMs > 0) {
                DurationBadge(
                    formatDuration(episode.durationMs),
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
            val progress = episode.progress
            if (progress != null && progress > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        },
    ) {
        Text(
            episode.label,
            style = BvType.CardTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 加载失败 / 空白时的整页占位。 */
@Composable
internal fun DetailEmptyBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 13sp 行高 21 的简介文字（电视）。 */
internal val TvDescriptionStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 21.sp)

/** 电视 UP 名字（13sp Bold）。 */
internal val TvUpNameStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)

/** 平板「视频详情」页头（15sp Bold）。 */
internal val TabletHeaderStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
