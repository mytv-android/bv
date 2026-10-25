package dev.aaa1115910.bv.ui.feature.video

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.FullPageLoading
import dev.aaa1115910.bv.ui.designsystem.component.PageMessage
import dev.aaa1115910.bv.ui.designsystem.icon.BvIcons
import dev.aaa1115910.bv.ui.designsystem.theme.CoverTheme
import dev.aaa1115910.bv.ui.feature.player.PlayRequest
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoActionsViewModel
import kotlinx.coroutines.launch

/**
 * 视频详情与番剧详情共用的页面骨架：CoverTheme、加载 / 失败占位、三种布局分派、
 * 投币 / 收藏 / 分 P 抽屉 / 回复 / 图片预览 / 简介等弹出层，以及操作结果提示。
 *
 * @param ui 界面数据，null 表示还在加载
 * @param likes / coins / favorites 视频统计数（番剧传 null）
 * @param tvPlayLabel / onTvPlay 电视「▶ 继续 … / ▶ 播放」按钮
 * @param tvLastAction 电视第 5 个图标按钮（视频：UP 主；番剧：追番）
 */
@Composable
internal fun DetailPage(
    ui: DetailUi?,
    loadError: String?,
    onRetry: () -> Unit,
    follow: DetailFollow?,
    actionsVm: VideoActionsViewModel,
    likes: Int?,
    coins: Int?,
    favorites: Int?,
    shareTitle: String,
    shareUrl: String,
    commentVm: CommentViewModel,
    playerVm: VideoPlayerV3ViewModel,
    holder: DetailPlaybackHolder,
    videoDetailRepository: VideoDetailRepository,
    handlers: DetailHandlers,
    touchCurrentCid: Long,
    tvCurrentCid: Long,
    tvPlayLabel: String,
    onTvPlay: () -> Unit,
    tvLastAction: TvDetailAction,
    ownerName: String,
    onBeforePlay: (PlayRequest) -> Unit,
    onPlayerLoadNewVideo: (VideoListItemData) -> Unit,
) {
    val profile = LocalDeviceProfile.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogs = remember { DetailDialogs() }
    val pictures = rememberCommentPictures()
    var showReplies by remember { mutableStateOf(false) }

    LaunchedEffect(actionsVm) {
        actionsVm.events.collect { event -> context.showActionEvent(event) }
    }

    val onShare: () -> Unit = { shareVideo(context, shareTitle, shareUrl) }
    val sendDanmakuUnsupported = stringResource(R.string.video_detail_send_danmaku_unsupported)
    val callbacks = remember(handlers, dialogs, commentVm, scope, pictures, context, sendDanmakuUnsupported) {
        DetailCallbacks(
            onBack = handlers.onBack,
            onUpClick = handlers.onUpClick,
            onFollowClick = handlers.onFollowClick,
            onPlay = handlers.onPlay,
            onOpenEpisodes = { row -> dialogs.episodesRow = row.key },
            onSeasonSelect = handlers.onSeasonSelect,
            onRelatedClick = handlers.onRelatedClick,
            onTagClick = handlers.onTagClick,
            onShowPictures = { list, index -> scope.launch { pictures.show(list, index) } },
            onOpenReplies = { comment ->
                commentVm.prepareReplies(comment)
                showReplies = true
                scope.launch { commentVm.refreshReplies() }
            },
            onSendDanmaku = { Toast.makeText(context, sendDanmakuUnsupported, Toast.LENGTH_SHORT).show() },
        )
    }

    CoverTheme(ui?.cover, forceDark = profile.isTv) {
        Box(Modifier.fillMaxSize()) {
            when {
                ui == null && loadError != null -> DetailEmptyBox {
                    PageMessage(message = loadError, onRetry = onRetry)
                }

                ui == null -> DetailEmptyBox { FullPageLoading() }

                profile.isTv -> {
                    val likeLabel = stringResource(R.string.video_detail_like)
                    val coinLabel = stringResource(R.string.video_detail_coin)
                    val favoriteLabel = stringResource(R.string.video_detail_favorite)
                    val toViewLabel = stringResource(R.string.video_detail_to_view)
                    VideoDetailTv(
                        ui = ui,
                        playLabel = tvPlayLabel,
                        onPlayDefault = onTvPlay,
                        actions = listOf(
                            TvDetailAction(
                                icon = if (actionsVm.liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = likeLabel,
                                checked = actionsVm.liked,
                                onClick = { actionsVm.toggleLike() },
                            ),
                            TvDetailAction(
                                icon = BvIcons.Coin,
                                contentDescription = coinLabel,
                                checked = actionsVm.coinCount > 0,
                                onClick = { onCoinClick(actionsVm, dialogs) },
                            ),
                            TvDetailAction(
                                icon = if (actionsVm.favoured) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = favoriteLabel,
                                checked = actionsVm.favoured,
                                onClick = { onFavoriteClick(actionsVm, dialogs) },
                            ),
                            TvDetailAction(
                                icon = if (actionsVm.inToView) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
                                contentDescription = toViewLabel,
                                checked = actionsVm.inToView,
                                onClick = { actionsVm.toggleToView() },
                            ),
                            tvLastAction,
                        ),
                        currentCid = tvCurrentCid,
                        callbacks = callbacks,
                        onShowDescription = { dialogs.description = true },
                    )
                }

                else -> {
                    val actionItems = rememberDetailActionItems(
                        vm = actionsVm,
                        likes = likes,
                        coins = coins,
                        favorites = favorites,
                        dialogs = dialogs,
                        onShare = onShare,
                    )
                    TouchDetailHost(
                        layout = profile.layout,
                        holder = holder,
                        playerVm = playerVm,
                        args = EmbeddedPlayerArgs(
                            cover = ui.cover,
                            ownerName = ownerName,
                            isTablet = profile.layout == DeviceLayout.Tablet,
                        ),
                        videoDetailRepository = videoDetailRepository,
                        onBeforePlay = onBeforePlay,
                        onPlayerLoadNewVideo = onPlayerLoadNewVideo,
                        onExit = handlers.onBack,
                        onEmbeddedMore = { dialogs.more = true },
                        phone = { slot ->
                            VideoDetailPhone(
                                ui = ui,
                                follow = follow,
                                actionItems = actionItems,
                                currentCid = touchCurrentCid,
                                commentVm = commentVm,
                                callbacks = callbacks,
                                playerSlot = slot,
                            )
                        },
                        tablet = { slot ->
                            VideoDetailTablet(
                                ui = ui,
                                follow = follow,
                                actionItems = actionItems,
                                currentCid = touchCurrentCid,
                                commentVm = commentVm,
                                callbacks = callbacks,
                                playerSlot = slot,
                            )
                        },
                    )
                }
            }

            // ---------------------------------------------------------------- 弹出层
            if (dialogs.coin) {
                CoinDialog(actionsVm) { dialogs.coin = false }
            }
            if (dialogs.favorite) {
                FavoriteDialog(actionsVm) { dialogs.favorite = false }
            }
            if (ui != null) {
                val rowKey = dialogs.episodesRow
                if (rowKey != null && ui.rows.isNotEmpty()) {
                    EpisodeSheet(
                        rows = ui.rows,
                        initialRowKey = rowKey,
                        currentCid = touchCurrentCid,
                        onPlay = { row, episode -> handlers.onPlay(row, episode) },
                        onDismiss = { dialogs.episodesRow = null },
                    )
                }
                if (dialogs.description && profile.isTv) {
                    TvDescriptionDialog(
                        title = ui.title,
                        description = ui.description,
                        onDismiss = { dialogs.description = false },
                    )
                }
            }
            if (dialogs.more) {
                DetailMoreSheet(actionsVm, onShare = onShare, onDismiss = { dialogs.more = false })
            }
            if (showReplies) {
                RepliesSheet(
                    commentVm = commentVm,
                    onShowPictures = callbacks.onShowPictures,
                    onDismiss = { showReplies = false },
                )
            }
            if (!profile.isTv) {
                CommentPicturePreviewer(pictures)
            }
        }
    }
}
