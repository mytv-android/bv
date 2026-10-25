package dev.aaa1115910.bv.ui.feature.video

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.feature.player.PlayRequest
import dev.aaa1115910.bv.ui.feature.player.PlayerLaunchExtras
import dev.aaa1115910.bv.ui.navigation.PlayerRoute
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.ui.navigation.TagRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoActionsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * 番剧详情（第 9.11 节）：SeasonViewModel（旧电视 SeasonInfoScreen），布局完全套用视频详情：
 * 分 P 行换成剧集行（「第 N 话 {标题}」），多季时标题下加季度选择行；关注按钮为追番。
 * 点赞 / 投币 / 收藏 / 稍后再看 / 评论针对当前剧集（aid）。
 */
@Composable
fun SeasonScreen(nav: NavController, route: SeasonRoute) {
    val seasonVm: SeasonViewModel = koinViewModel()
    val commentVm: CommentViewModel = koinViewModel()
    val actionsVm: VideoActionsViewModel = koinViewModel()
    val playerVm: VideoPlayerV3ViewModel = koinViewModel()
    val holder: DetailPlaybackHolder = viewModel { DetailPlaybackHolder() }
    val videoInfoRepository: VideoInfoRepository = koinInject()
    val videoDetailRepository: VideoDetailRepository = koinInject()
    val profile = LocalDeviceProfile.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var reloadTick by remember { mutableIntStateOf(0) }

    val season = seasonVm.seasonData
    val routeKey = "season:${route.seasonId}:${route.epId}"

    // ---------------------------------------------------------------- 加载（旧电视 SeasonInfoScreen）
    LaunchedEffect(routeKey, reloadTick) {
        if (holder.loadedKey == routeKey && seasonVm.seasonData != null) {
            // 从播放页返回：更新追番进度（旧版 ON_RESUME 逻辑）
            seasonVm.updateLastPlayProgress()
            return@LaunchedEffect
        }
        if (route.seasonId == null && route.epId == null) {
            nav.popBackStack()
            return@LaunchedEffect
        }
        holder.loadError = null
        seasonVm.tip = "Loading"
        seasonVm.epId = route.epId
        seasonVm.seasonId = route.seasonId
        seasonVm.updateSeasonData()
        if (seasonVm.seasonData != null) {
            holder.loadedKey = routeKey
        } else {
            holder.loadError = seasonVm.tip
        }
    }

    // 播放某一集：手机 / 平板交给内嵌播放器，电视进入 PlayerRoute
    val playEpisode: (SeasonDetail, List<Episode>, Episode, Long) -> Unit = { data, list, episode, startMs ->
        applySeasonVideoList(data, list, videoInfoRepository)
        val request = episodeRequest(context, data, episode, startMs)
        if (profile.isTv) {
            PlayerLaunchExtras.put(request.aid, data.cover, "")
            nav.navigate(
                PlayerRoute(
                    aid = request.aid,
                    cid = request.cid,
                    title = request.title,
                    partTitle = request.partTitle,
                    played = request.startMs.toInt(),
                    fromSeason = true,
                    subType = data.subType,
                    epid = episode.id,
                    seasonId = data.seasonId,
                    proxyArea = seasonVm.proxyArea.ordinal,
                    playerIconIdle = request.playerIconIdle,
                    playerIconMoving = request.playerIconMoving,
                )
            )
        } else {
            holder.play(request)
        }
    }

    // 默认播放：有播放记录时从上次的剧集与进度继续，否则第一集正片（旧电视 SeasonInfoPart.onPlay）
    val playDefault: (SeasonDetail) -> Unit = { data ->
        val progress = seasonVm.lastPlayProgress
        val target = findEpisode(data, progress?.lastEpId)
        when {
            target != null -> playEpisode(
                data, target.first, target.second,
                ((progress?.lastTime ?: 0) * 1000L).coerceAtLeast(0L),
            )

            data.episodes.isNotEmpty() -> playEpisode(data, data.episodes, data.episodes.first(), 0L)
            else -> Toast.makeText(context, R.string.video_detail_no_episode, Toast.LENGTH_SHORT).show()
        }
    }

    // 当前剧集（评论、操作按钮针对它）
    val currentEpisodeAid = holder.request?.aid
        ?: findEpisode(season, seasonVm.lastPlayProgress?.lastEpId)?.second?.aid
        ?: season?.episodes?.firstOrNull()?.aid
        ?: 0L

    LaunchedEffect(season?.seasonId) {
        val data = season ?: return@LaunchedEffect
        // 手机 / 平板：内嵌播放器直接开始播放；切换季度后播放新季度
        if (!profile.isTv) {
            val playing = holder.request
            val playingInSeason = playing != null && allEpisodes(data).any { it.cid == playing.cid }
            if (!playingInSeason) playDefault(data)
        }
    }

    LaunchedEffect(currentEpisodeAid) {
        if (currentEpisodeAid == 0L) return@LaunchedEffect
        actionsVm.load(aid = currentEpisodeAid, upMid = null)
        if (commentVm.commentId != currentEpisodeAid) {
            commentVm.commentType = 1L
            commentVm.commentId = currentEpisodeAid
            launch { commentVm.refreshComments() }
        }
    }

    val progress = seasonVm.lastPlayProgress
    val ui = remember(season, progress, context) {
        season?.let { data ->
            buildSeasonUi(
                context = context,
                data = data,
                lastEpId = progress?.lastEpId,
                lastTimeSec = progress?.lastTime ?: 0,
            )
        }
    }

    val handlers = remember(season, progress, seasonVm, scope, lifecycleOwner, nav, playEpisode) {
        DetailHandlers(
            onBack = {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) nav.popBackStack()
            },
            onUpClick = {},
            onFollowClick = {
                scope.launch {
                    if (seasonVm.isFollowing) seasonVm.unFollowSeason() else seasonVm.followSeason()
                }
            },
            onPlay = { row, episode ->
                val data = season
                if (data != null) {
                    val list = rowEpisodes(data, row.key)
                    val target = list.firstOrNull { it.cid == episode.cid && it.id == episode.epid }
                        ?: list.firstOrNull { it.cid == episode.cid }
                    if (target != null) {
                        val startMs = if (target.id == progress?.lastEpId) {
                            ((progress?.lastTime ?: 0) * 1000L).coerceAtLeast(0L)
                        } else 0L
                        playEpisode(data, list, target, startMs)
                    }
                }
            },
            onSeasonSelect = { seasonId ->
                if ((seasonVm.seasonData?.seasonId ?: 0) != seasonId) {
                    // 不清空当前数据：手机 / 平板的内嵌播放器继续播放，新季度加载完成后切换
                    seasonVm.seasonId = seasonId
                    seasonVm.epId = null
                    scope.launch { seasonVm.updateSeasonData() }
                }
            },
            onRelatedClick = { video -> nav.openVideo(video) },
            onTagClick = { tag -> nav.navigate(TagRoute(tag.id)) },
        )
    }

    // 电视「▶ 继续 第 N 话 · {mm:ss}」/「▶ 播放」
    val lastEpisode = findEpisode(season, progress?.lastEpId)?.second
    val tvPlayLabel = if (lastEpisode != null && (progress?.lastTime ?: 0) > 0) {
        stringResource(
            R.string.video_detail_continue,
            episodeNumber(context, lastEpisode),
            formatDuration((progress?.lastTime ?: 0) * 1000L),
        )
    } else {
        stringResource(R.string.video_detail_play)
    }

    val currentEpid = holder.request?.epid ?: lastEpisode?.id
    DetailPage(
        ui = ui,
        loadError = holder.loadError,
        onRetry = { reloadTick++ },
        follow = DetailFollow(
            label = stringResource(R.string.video_detail_follow_season),
            followedLabel = stringResource(R.string.video_detail_followed_season),
            followed = if (actionsVm.isLoggedIn) seasonVm.isFollowing else null,
        ),
        actionsVm = actionsVm,
        likes = null,
        coins = null,
        favorites = null,
        shareTitle = season?.title.orEmpty(),
        shareUrl = EpisodeUrlPrefix + (currentEpid ?: 0),
        commentVm = commentVm,
        playerVm = playerVm,
        holder = holder,
        videoDetailRepository = videoDetailRepository,
        handlers = handlers,
        touchCurrentCid = playerVm.currentCid,
        tvCurrentCid = lastEpisode?.cid ?: 0L,
        tvPlayLabel = tvPlayLabel,
        onTvPlay = { season?.let(playDefault) },
        tvLastAction = TvDetailAction(
            icon = if (seasonVm.isFollowing) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.video_detail_follow_season),
            checked = seasonVm.isFollowing,
            onClick = { handlers.onFollowClick() },
        ),
        ownerName = "",
        onBeforePlay = { request ->
            val data = season
            if (data != null) {
                val list = if (data.episodes.any { it.cid == request.cid }) {
                    data.episodes
                } else {
                    data.sections.map { it.episodes }.firstOrNull { list -> list.any { it.cid == request.cid } }
                        ?: data.episodes
                }
                applySeasonVideoList(data, list, videoInfoRepository)
            }
        },
        onPlayerLoadNewVideo = { item ->
            val data = season
            holder.play(
                PlayRequest(
                    aid = item.aid,
                    cid = item.cid,
                    title = data?.title.orEmpty(),
                    partTitle = item.title,
                    epid = item.epid,
                    seasonId = item.seasonId ?: data?.seasonId,
                    fromSeason = true,
                    subType = data?.subType ?: 0,
                    playerIconIdle = data?.playerIcon?.idle ?: "",
                    playerIconMoving = data?.playerIcon?.moving ?: "",
                )
            )
        },
    )
}

/** 「第 N 话」（标题不是数字时原样使用，如 SP）。 */
private fun episodeNumber(context: Context, episode: Episode): String =
    episode.title.toIntOrNull()?.let { context.getString(R.string.video_detail_episode_number, it.toString()) }
        ?: episode.title

/** 剧集标签「第 N 话 {标题}」。 */
private fun episodeLabel(context: Context, episode: Episode): String =
    listOf(episodeNumber(context, episode), episode.longTitle).filter { it.isNotBlank() }.joinToString(" ")

private fun allEpisodes(data: SeasonDetail): List<Episode> =
    data.episodes + data.sections.flatMap { it.episodes }

/** 在正片与其他分组里找剧集 [epId]，返回（所在列表, 剧集）。 */
private fun findEpisode(data: SeasonDetail?, epId: Int?): Pair<List<Episode>, Episode>? {
    if (data == null || epId == null) return null
    data.episodes.firstOrNull { it.id == epId }?.let { return data.episodes to it }
    data.sections.forEach { section ->
        section.episodes.firstOrNull { it.id == epId }?.let { return section.episodes to it }
    }
    return null
}

private const val FeatureRowKey = "episodes"
private const val SectionRowPrefix = "section-"

private fun rowEpisodes(data: SeasonDetail, rowKey: String): List<Episode> = when {
    rowKey == FeatureRowKey -> data.episodes
    rowKey.startsWith(SectionRowPrefix) ->
        data.sections.getOrNull(rowKey.removePrefix(SectionRowPrefix).toIntOrNull() ?: -1)?.episodes ?: emptyList()

    else -> emptyList()
}

private fun episodeRequest(context: Context, data: SeasonDetail, episode: Episode, startMs: Long) = PlayRequest(
    aid = episode.aid,
    cid = episode.cid,
    title = data.title,
    partTitle = episodeLabel(context, episode),
    epid = episode.id,
    seasonId = data.seasonId,
    fromSeason = true,
    subType = data.subType,
    startMs = startMs,
    playerIconIdle = data.playerIcon?.idle ?: "",
    playerIconMoving = data.playerIcon?.moving ?: "",
)

/** 播放器的选集列表（旧电视 SeasonInfoScreen：VideoListPgcEpisode「第 N 集 {标题}」）。 */
private fun applySeasonVideoList(
    data: SeasonDetail,
    episodes: List<Episode>,
    videoInfoRepository: VideoInfoRepository,
) {
    val list = episodes.mapIndexed { index, episode ->
        VideoListPgcEpisode(
            aid = episode.aid,
            cid = episode.cid,
            epid = episode.id,
            seasonId = data.seasonId,
            title = listOf(episode.title, episode.longTitle).filter { it.isNotBlank() }.joinToString(" "),
            index = index,
        )
    }
    videoInfoRepository.replaceVideoList(list)
}

private fun buildSeasonUi(
    context: Context,
    data: SeasonDetail,
    lastEpId: Int?,
    lastTimeSec: Int,
): DetailUi {
    fun Episode.toDetail() = DetailEpisode(
        aid = aid,
        cid = cid,
        epid = id,
        title = episodeLabel(context, this),
        label = episodeLabel(context, this),
        cover = cover.ifEmpty { data.cover },
        durationMs = duration.toLong(),
        progress = if (id == lastEpId) watchedFraction(lastTimeSec, duration / 1000) else null,
    )

    val rows = buildList<DetailEpisodeRow> {
        if (data.episodes.isNotEmpty()) {
            add(
                DetailEpisodeRow(
                    key = FeatureRowKey,
                    title = context.getString(R.string.video_detail_season_feature),
                    action = context.getString(R.string.video_detail_all_more),
                    episodes = data.episodes.map { it.toDetail() },
                )
            )
        }
        data.sections.forEachIndexed { index, section ->
            if (section.episodes.isNotEmpty()) {
                add(
                    DetailEpisodeRow(
                        key = "$SectionRowPrefix$index",
                        title = section.title,
                        action = context.getString(R.string.video_detail_all_more),
                        episodes = section.episodes.map { it.toDetail() },
                    )
                )
            }
        }
    }
    val styles = data.styles.joinToString(" · ")
    val publish = data.publish.publishDate.takeIf { it.isNotBlank() }
    return DetailUi(
        cover = data.cover,
        title = data.title,
        eyebrow = listOf(styles, data.newEpDesc).filter { it.isNotBlank() }.joinToString(" · "),
        description = data.description,
        tags = emptyList(),
        upName = styles.ifEmpty { data.title },
        upFace = data.cover,
        upMid = 0L,
        upMeta = data.newEpDesc,
        metaPhone = listOfNotNull(publish, data.originTitle?.takeIf { it.isNotBlank() }).joinToString(" · "),
        metaTablet = listOfNotNull(data.newEpDesc.takeIf { it.isNotBlank() }, publish).joinToString(" · "),
        metaTv = listOfNotNull(publish).joinToString(" · "),
        rows = rows,
        seasons = data.seasons.map { DetailSeasonChip(it.seasonId, it.title?.takeIf { t -> t.isNotBlank() } ?: it.shortTitle) },
        currentSeasonId = data.seasonId,
    )
}
