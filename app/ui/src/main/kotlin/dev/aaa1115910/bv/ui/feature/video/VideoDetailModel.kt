package dev.aaa1115910.bv.ui.feature.video

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.Picture
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.feature.player.PlayRequest
import dev.aaa1115910.bv.viewmodel.video.VideoAction
import dev.aaa1115910.bv.viewmodel.video.VideoActionEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 一个可播放的分 P / 合集视频 / 剧集。 */
@Immutable
internal data class DetailEpisode(
    val aid: Long,
    val cid: Long,
    /** 番剧剧集 id（普通视频为 null） */
    val epid: Int?,
    /** 分 P 名 / 剧集名（播放器的 partTitle） */
    val title: String,
    /** 完整标签：「P1 标题」「第 1 话 标题」 */
    val label: String,
    val cover: String,
    val durationMs: Long,
    /** 已看比例（最后观看的那一集），null 表示没有记录 */
    val progress: Float? = null,
)

/** 一行分 P / 剧集（手机 / 平板是标签行，电视是卡片行）。 */
@Immutable
internal data class DetailEpisodeRow(
    val key: String,
    val title: String,
    /** 标题右侧的动作文字（「5P ›」「全部 ›」），null 表示不显示 */
    val action: String?,
    val episodes: List<DetailEpisode>,
)

@Immutable
internal data class DetailSeasonChip(val seasonId: Int, val title: String)

/** 视频详情 / 番剧详情共用的界面数据（第 9.3、9.11 节：番剧详情完全套用视频详情的布局）。 */
@Immutable
internal data class DetailUi(
    val cover: String,
    val title: String,
    /** 电视 Eyebrow「{分区} · {标签} · {n}P」 */
    val eyebrow: String,
    val description: String,
    val tags: List<Tag>,
    /** UP 主行：名字 / 头像 / mid（番剧为 0）；番剧显示类型与更新信息 */
    val upName: String,
    val upFace: String,
    val upMid: Long,
    /** 手机 UP 名下方的副信息（「粉丝 X」） */
    val upMeta: String,
    /** 手机标题下的信息行「{播放}播放 · {弹幕}弹幕 · {MM-dd} · {BV号}」 */
    val metaPhone: String,
    /** 平板 UP 名下方的信息行「粉丝 · 播放 · 日期」 */
    val metaTablet: String,
    /** 电视 UP 名右侧的信息行「粉丝 X · {播放}播放 · {点赞}点赞 · {投币}投币」 */
    val metaTv: String,
    val rows: List<DetailEpisodeRow>,
    val seasons: List<DetailSeasonChip> = emptyList(),
    val currentSeasonId: Int = 0,
    val related: List<VideoCardData> = emptyList(),
    val commentCount: Int = 0,
)

/** 关注 / 追番按钮。[followed] 为 null 时不显示（未登录或状态未知）。 */
@Immutable
internal data class DetailFollow(
    val label: String,
    val followedLabel: String,
    val followed: Boolean?,
)

/** 由视频详情 / 番剧详情页面提供的业务回调。 */
@Stable
internal class DetailHandlers(
    val onBack: () -> Unit,
    val onUpClick: () -> Unit,
    val onFollowClick: () -> Unit,
    val onPlay: (DetailEpisodeRow, DetailEpisode) -> Unit,
    val onSeasonSelect: (Int) -> Unit,
    val onRelatedClick: (VideoCardData) -> Unit,
    val onTagClick: (Tag) -> Unit,
)

/** 详情页三种布局共用的回调（业务回调 + 弹出层）。 */
@Stable
internal class DetailCallbacks(
    val onBack: () -> Unit,
    val onUpClick: () -> Unit,
    val onFollowClick: () -> Unit,
    val onPlay: (DetailEpisodeRow, DetailEpisode) -> Unit,
    val onOpenEpisodes: (DetailEpisodeRow) -> Unit,
    val onSeasonSelect: (Int) -> Unit,
    val onRelatedClick: (VideoCardData) -> Unit,
    val onTagClick: (Tag) -> Unit,
    val onShowPictures: (List<Picture>, Int) -> Unit,
    val onOpenReplies: (Comment) -> Unit,
    val onSendDanmaku: () -> Unit,
)

/**
 * 详情页的 UI 状态（不属于任何旧 ViewModel 的部分），按导航目的地保存：
 * 要播放的分 P、内嵌播放器的进度（离开页面后回来从这里继续）、全屏、历史记录。
 */
internal class DetailPlaybackHolder : ViewModel() {
    /** 内嵌播放器要播放的内容，null 表示还没开始 */
    var request by mutableStateOf<PlayRequest?>(null)
        private set

    /** 每次 [play] 加一，内嵌播放器据此重新开始播放 */
    var requestId by mutableIntStateOf(0)
        private set

    /** 最近一次记录的播放进度与分 P（内嵌播放器被重建时从这里继续） */
    var lastPositionMs = 0L
    var lastCid = 0L

    var fullscreen by mutableStateOf(false)

    /** 已加载的内容（aid / 番剧 id），重新进入页面时不重复加载 */
    var loadedKey: Any? = null
    var loadError by mutableStateOf<String?>(null)

    /** 历史记录（电视「继续 P{n} · {mm:ss}」、分 P 卡片进度条） */
    var lastPlayedCid by mutableLongStateOf(0L)
    var lastPlayedTimeSec by mutableIntStateOf(0)
    var historyReady = false

    fun play(request: PlayRequest) {
        this.request = request
        lastPositionMs = request.startMs
        lastCid = request.cid
        requestId++
    }
}

// ---------------------------------------------------------------- 格式化

/** 「58.2万」「1.3亿」 */
internal fun Context.formatCount(count: Int): String = when {
    count >= 100_000_000 -> getString(R.string.video_detail_count_yi, trimDecimal(count / 100_000_000.0))
    count >= 10_000 -> getString(R.string.video_detail_count_wan, trimDecimal(count / 10_000.0))
    else -> count.coerceAtLeast(0).toString()
}

private fun trimDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value).removeSuffix(".0")

/** 今年的日期「MM-dd」，往年「yyyy-MM-dd」 */
internal fun formatShortDate(date: Date): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val pattern = if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) "MM-dd" else "yyyy-MM-dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

/** 「06:12」「1:02:03」 */
internal fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/** 已看比例；[playedSec] < 0 表示看完 */
internal fun watchedFraction(playedSec: Int, durationSec: Int): Float? = when {
    playedSec < 0 -> 1f
    playedSec == 0 || durationSec <= 0 -> null
    else -> (playedSec.toFloat() / durationSec).coerceIn(0f, 1f)
}

// ---------------------------------------------------------------- 分享与提示

internal fun shareVideo(context: Context, title: String, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title $url")
    }
    try {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.video_detail_share_to))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.video_detail_action_failed_plain, Toast.LENGTH_SHORT).show()
    }
}

internal const val VideoUrlPrefix = "https://www.bilibili.com/video/"
internal const val EpisodeUrlPrefix = "https://www.bilibili.com/bangumi/play/ep"

/** 把 [VideoActionEvent] 显示为 Toast（点赞成功不提示，按钮本身已变化）。 */
internal fun Context.showActionEvent(event: VideoActionEvent) {
    val text: String? = when (event) {
        is VideoActionEvent.LoginRequired -> getString(R.string.video_detail_login_required)
        is VideoActionEvent.Failed -> event.message?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.video_detail_action_failed, it) }
            ?: getString(R.string.video_detail_action_failed_plain)

        is VideoActionEvent.Succeeded -> when (event.action) {
            VideoAction.Coin -> getString(R.string.video_detail_coin_done)
            VideoAction.Favorite -> getString(R.string.video_detail_favorite_done)
            VideoAction.AddToView -> getString(R.string.video_detail_toview_added)
            VideoAction.DelToView -> getString(R.string.video_detail_toview_removed)
            VideoAction.Follow -> getString(R.string.video_detail_follow_done)
            VideoAction.Unfollow -> getString(R.string.video_detail_unfollow_done)
            VideoAction.Like, VideoAction.Unlike -> null
        }
    }
    if (text != null) Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
