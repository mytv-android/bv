package dev.aaa1115910.bv.ui.feature.video

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.feature.player.PlayRequest
import dev.aaa1115910.bv.ui.feature.player.PlayerLaunchExtras
import dev.aaa1115910.bv.ui.navigation.PlayerRoute
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.ui.navigation.SpaceRoute
import dev.aaa1115910.bv.ui.navigation.TagRoute
import dev.aaa1115910.bv.ui.navigation.VideoRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoActionsViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * 视频详情（第 9.3 节）。数据照抄旧电视 VideoInfoScreen / 旧手机 VideoPlayerActivity：
 * VideoDetailViewModel 加载详情（失败显示重试），PGC 内容（redirect 到 ep）替换为 SeasonRoute；
 * 评论用 CommentViewModel；点赞 / 投币 / 收藏 / 稍后再看 / 关注用 VideoActionsViewModel。
 * 手机 / 平板内嵌播放器直接播放（有历史记录时从上次的分 P 与进度继续）；电视「播放」进入 PlayerRoute。
 */
@Composable
fun VideoDetailScreen(nav: NavController, route: VideoRoute) {
    val detailVm: VideoDetailViewModel = koinViewModel()
    val commentVm: CommentViewModel = koinViewModel()
    val actionsVm: VideoActionsViewModel = koinViewModel()
    val playerVm: VideoPlayerV3ViewModel = koinViewModel()
    val holder: DetailPlaybackHolder = viewModel { DetailPlaybackHolder() }
    val videoInfoRepository: VideoInfoRepository = koinInject()
    val videoDetailRepository: VideoDetailRepository = koinInject()
    val profile = LocalDeviceProfile.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reloadTick by remember { mutableIntStateOf(0) }

    val detail = detailVm.videoDetail

    // ---------------------------------------------------------------- 加载
    LaunchedEffect(route.aid, reloadTick) {
        if (holder.loadedKey == route.aid && detailVm.videoDetail != null) {
            // 重新进入页面（例如从播放页返回）：更新历史记录（旧电视 VideoInfoScreen 的 ON_RESUME 逻辑）
            detailVm.loadDetailOnlyUpdateHistory(route.aid)
            detailVm.videoDetail?.history?.let {
                holder.lastPlayedCid = it.lastPlayedCid
                holder.lastPlayedTimeSec = it.progress
            }
            return@LaunchedEffect
        }
        holder.loadError = null
        runCatching { detailVm.loadDetail(route.aid) }
            .onSuccess { holder.loadedKey = route.aid }
            .onFailure { holder.loadError = it.localizedMessage ?: it.javaClass.simpleName }
    }

    val playVideoList: (VideoDetail, Long) -> Unit = { d, cid -> applyVideoList(d, cid, detailVm, videoInfoRepository) }

    LaunchedEffect(detail) {
        val d = detail ?: return@LaunchedEffect
        // PGC 内容：替换为番剧详情
        if (d.redirectToEp && d.epid != null) {
            nav.navigate(SeasonRoute(epId = d.epid)) {
                popUpTo<VideoRoute> { inclusive = true }
            }
            return@LaunchedEffect
        }
        if (!holder.historyReady) {
            holder.lastPlayedCid = d.history.lastPlayedCid
            holder.lastPlayedTimeSec = d.history.progress
            holder.historyReady = true
        }
        if (commentVm.commentId != d.aid) {
            commentVm.commentType = 1L
            commentVm.commentId = d.aid
            launch { commentVm.refreshComments() }
        }
        actionsVm.load(
            aid = d.aid,
            upMid = d.author.mid,
            initialLiked = d.userActions.like,
            initialFavoured = d.userActions.favorite,
            initialCoined = d.userActions.coin,
        )
        // 手机 / 平板：内嵌播放器直接开始播放（有历史记录时从上次的分 P 与进度继续）
        if (!profile.isTv && holder.request == null) {
            val page = d.pages.firstOrNull { it.cid == d.history.lastPlayedCid } ?: d.pages.firstOrNull()
            if (page != null) {
                val start = if (page.cid == d.history.lastPlayedCid) d.history.progress * 1000L else 0L
                holder.play(pageRequest(d, page, start.coerceAtLeast(0L)))
            }
        }
    }

    val ui = detail?.takeUnless { it.redirectToEp }?.let { d ->
        buildVideoDetailUi(
            context = context,
            d = d,
            related = detailVm.relatedVideos.toList(),
            followerCount = actionsVm.followerCount,
            likeDelta = actionsVm.likeDelta,
            coinDelta = actionsVm.coinDelta,
            lastPlayedCid = holder.lastPlayedCid,
            lastPlayedTimeSec = holder.lastPlayedTimeSec,
        )
    }

    val navigateToPlayer: (VideoDetail, PlayRequest) -> Unit = { d, request ->
        PlayerLaunchExtras.put(request.aid, d.cover, d.author.name)
        nav.navigate(
            PlayerRoute(
                aid = request.aid,
                cid = request.cid,
                title = request.title,
                partTitle = request.partTitle,
                played = request.startMs.toInt(),
                fromSeason = false,
                isVerticalVideo = d.containsVerticalVideo(),
                playerIconIdle = request.playerIconIdle,
                playerIconMoving = request.playerIconMoving,
            )
        )
    }

    val handlers = DetailHandlers(
        onBack = {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) nav.popBackStack()
        },
        onUpClick = { detail?.author?.mid?.takeIf { it > 0 }?.let { nav.navigate(SpaceRoute(it)) } },
        onFollowClick = { actionsVm.toggleFollow() },
        onPlay = { row, episode ->
            val d = detail
            if (d != null) {
                playVideoList(d, episode.cid)
                val ugc = d.ugcSeason
                val start = if (episode.cid == holder.lastPlayedCid) holder.lastPlayedTimeSec * 1000L else 0L
                val request = PlayRequest(
                    aid = episode.aid,
                    cid = episode.cid,
                    title = if (ugc != null && row.key.startsWith(UgcRowPrefix)) ugc.title else d.title,
                    partTitle = episode.title,
                    startMs = start.coerceAtLeast(0L),
                    playerIconIdle = d.playerIcon?.idle ?: "",
                    playerIconMoving = d.playerIcon?.moving ?: "",
                )
                if (profile.isTv) navigateToPlayer(d, request) else holder.play(request)
            }
        },
        onSeasonSelect = {},
        onRelatedClick = { video -> nav.openVideo(video) },
        onTagClick = { tag -> nav.navigate(TagRoute(tag.id)) },
    )

    // 电视「▶ 继续 P{n} · {mm:ss}」/「▶ 播放」
    val lastPageIndex = detail?.pages?.indexOfFirst { it.cid == holder.lastPlayedCid } ?: -1
    val tvPlayLabel = if (lastPageIndex >= 0 && holder.lastPlayedTimeSec > 0) {
        stringResource(
            R.string.video_detail_continue,
            stringResource(R.string.video_detail_part_short, lastPageIndex + 1),
            formatDuration(holder.lastPlayedTimeSec * 1000L),
        )
    } else {
        stringResource(R.string.video_detail_play)
    }
    val onTvPlay: () -> Unit = {
        val d = detail
        val page = d?.pages?.firstOrNull { it.cid == holder.lastPlayedCid } ?: d?.pages?.firstOrNull()
        if (d != null && page != null) {
            playVideoList(d, page.cid)
            val start = if (page.cid == holder.lastPlayedCid) holder.lastPlayedTimeSec * 1000L else 0L
            navigateToPlayer(d, pageRequest(d, page, start.coerceAtLeast(0L)))
        }
    }

    DetailPage(
        ui = ui,
        loadError = holder.loadError,
        onRetry = { reloadTick++ },
        follow = DetailFollow(
            label = stringResource(R.string.video_detail_follow),
            followedLabel = stringResource(R.string.video_detail_followed),
            followed = actionsVm.following,
        ),
        actionsVm = actionsVm,
        likes = detail?.stat?.like,
        coins = detail?.stat?.coin,
        favorites = detail?.stat?.favorite,
        shareTitle = detail?.title.orEmpty(),
        shareUrl = VideoUrlPrefix + detail?.bvid.orEmpty(),
        commentVm = commentVm,
        playerVm = playerVm,
        holder = holder,
        videoDetailRepository = videoDetailRepository,
        handlers = handlers,
        touchCurrentCid = playerVm.currentCid,
        tvCurrentCid = holder.lastPlayedCid,
        tvPlayLabel = tvPlayLabel,
        onTvPlay = onTvPlay,
        tvLastAction = TvDetailAction(
            icon = Icons.Outlined.Person,
            contentDescription = stringResource(R.string.video_detail_up),
            checked = false,
            onClick = { handlers.onUpClick() },
        ),
        ownerName = detail?.author?.name.orEmpty(),
        onBeforePlay = { request -> detail?.let { playVideoList(it, request.cid) } },
        onPlayerLoadNewVideo = { item -> holder.play(item.toRequest(detail)) },
    )
}

private const val UgcRowPrefix = "ugc-"

private fun pageRequest(d: VideoDetail, page: VideoPage, startMs: Long) = PlayRequest(
    aid = d.aid,
    cid = page.cid,
    title = d.title,
    partTitle = page.title,
    startMs = startMs,
    playerIconIdle = d.playerIcon?.idle ?: "",
    playerIconMoving = d.playerIcon?.moving ?: "",
)

/** 播放器选集面板切换到另一项：交给内嵌播放器按新的请求播放。 */
private fun VideoListItemData.toRequest(d: VideoDetail?) = PlayRequest(
    aid = aid,
    cid = cid,
    title = d?.title.orEmpty(),
    partTitle = title,
    epid = epid,
    seasonId = seasonId,
    playerIconIdle = d?.playerIcon?.idle ?: "",
    playerIconMoving = d?.playerIcon?.moving ?: "",
)

private fun VideoDetail.containsVerticalVideo(): Boolean =
    pages.any { it.dimension.isVertical } ||
            (ugcSeason?.sections?.any { section -> section.episodes.any { it.dimension?.isVertical == true } } ?: false)

/**
 * 设置播放器的选集列表（VideoInfoRepository，旧电视 onClickCover 的逻辑）：
 * 有合集时用包含 [cid] 的合集分组，否则用分 P。
 */
private fun applyVideoList(
    d: VideoDetail,
    cid: Long,
    detailVm: VideoDetailViewModel,
    videoInfoRepository: VideoInfoRepository,
) {
    val ugc = d.ugcSeason
    if (ugc != null && ugc.sections.isNotEmpty() && detailVm.videoDetail === d) {
        val index = ugc.sections.indexOfFirst { section ->
            section.episodes.any { episode -> episode.cid == cid || episode.pages.any { it.cid == cid } }
        }.takeIf { it >= 0 } ?: 0
        detailVm.updateUgcSeasonSectionVideoList(index)
    } else {
        videoInfoRepository.videoList.clear()
        videoInfoRepository.videoList.addAll(
            d.pages.mapIndexed { index, page ->
                VideoListPart(aid = d.aid, cid = page.cid, title = page.title, index = index)
            }
        )
    }
}

/** 由视频详情构造界面数据。 */
private fun buildVideoDetailUi(
    context: Context,
    d: VideoDetail,
    related: List<dev.aaa1115910.bv.entity.carddata.VideoCardData>,
    followerCount: Int?,
    likeDelta: Int,
    coinDelta: Int,
    lastPlayedCid: Long,
    lastPlayedTimeSec: Int,
): DetailUi {
    val fans = followerCount?.let { context.getString(R.string.video_detail_fans, context.formatCount(it)) }
    val play = context.getString(R.string.video_detail_play_count, context.formatCount(d.stat.view))
    val danmaku = context.getString(R.string.video_detail_danmaku_count, context.formatCount(d.stat.danmaku))
    val likes = context.getString(R.string.video_detail_like_count, context.formatCount(d.stat.like + likeDelta))
    val coins = context.getString(R.string.video_detail_coin_count, context.formatCount(d.stat.coin + coinDelta))
    val date = formatShortDate(d.publishDate)

    val rows = buildList<DetailEpisodeRow> {
        if (d.pages.size > 1) {
            add(
                DetailEpisodeRow(
                    key = "parts",
                    title = context.getString(R.string.video_detail_parts),
                    action = context.getString(R.string.video_detail_parts_more, d.pages.size),
                    episodes = d.pages.mapIndexed { index, page ->
                        DetailEpisode(
                            aid = d.aid,
                            cid = page.cid,
                            epid = null,
                            title = page.title,
                            label = context.getString(R.string.video_detail_part_label, index + 1, page.title),
                            cover = d.cover,
                            durationMs = page.duration * 1000L,
                            progress = if (page.cid == lastPlayedCid) watchedFraction(lastPlayedTimeSec, page.duration) else null,
                        )
                    },
                )
            )
        }
        d.ugcSeason?.let { ugc ->
            ugc.sections.forEachIndexed { sectionIndex, section ->
                if (section.episodes.isEmpty()) return@forEachIndexed
                add(
                    DetailEpisodeRow(
                        key = "$UgcRowPrefix$sectionIndex",
                        title = if (ugc.sections.size == 1 || section.title.isBlank()) {
                            context.getString(R.string.video_detail_collection, ugc.title)
                        } else {
                            section.title
                        },
                        action = context.getString(R.string.video_detail_all_more),
                        episodes = section.episodes.map { episode ->
                            DetailEpisode(
                                aid = episode.aid,
                                cid = episode.cid,
                                epid = null,
                                title = episode.title,
                                label = episode.title,
                                cover = episode.cover.ifEmpty { d.cover },
                                durationMs = episode.duration * 1000L,
                                progress = if (episode.cid == lastPlayedCid) {
                                    watchedFraction(lastPlayedTimeSec, episode.duration)
                                } else null,
                            )
                        },
                    )
                )
            }
        }
    }

    val eyebrow = buildList<String> {
        d.tags.take(2).forEach { add(it.name) }
        if (d.pages.size > 1) add("${d.pages.size}P")
    }.joinToString(" · ")

    return DetailUi(
        cover = d.cover,
        title = d.title,
        eyebrow = eyebrow,
        description = d.description,
        tags = d.tags,
        upName = d.author.name,
        upFace = d.author.face,
        upMid = d.author.mid,
        upMeta = fans.orEmpty(),
        metaPhone = listOf(play, danmaku, date, d.bvid).filter { it.isNotEmpty() }.joinToString(" · "),
        metaTablet = listOfNotNull(fans, play, date).joinToString(" · "),
        metaTv = listOfNotNull(fans, play, likes, coins).joinToString(" · "),
        rows = rows,
        related = related,
        commentCount = d.stat.reply,
    )
}
