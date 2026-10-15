package dev.aaa1115910.bv.ui.feature.timeline

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.season.Timeline
import dev.aaa1115910.biliapi.entity.season.TimelineEp
import dev.aaa1115910.biliapi.entity.season.TimelineFilter
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.resizedImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.util.Calendar

/**
 * 放送时间表（同旧电视 AnimeTimelineScreen：SeasonRepository.getTimeline + Prefs.apiType）。
 * 网络请求在 IO 线程，结果由调用方在主线程写入状态。
 */
internal suspend fun fetchTimelines(repository: SeasonRepository, filter: TimelineFilter): List<Timeline> =
    withContext(Dispatchers.IO) {
        repository.getTimeline(filter = filter, preferApiType = Prefs.apiType)
    }

/** 今天是周几：1 = 周一 … 7 = 周日（与 Timeline.dayOfWeek 一致）。 */
internal fun todayDayOfWeek(): Int {
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (dow == Calendar.SUNDAY) 7 else dow - 1
}

/** 本周（周一到周日）第 [dayOfWeek] 天的时间表：以接口标记的「今天」为基准往前 / 往后数。 */
internal fun List<Timeline>.forWeekday(dayOfWeek: Int): Timeline? {
    val todayIndex = indexOfFirst { it.isToday }
    if (todayIndex >= 0) {
        val target = getOrNull(todayIndex + (dayOfWeek - this[todayIndex].dayOfWeek))
        if (target != null && target.dayOfWeek == dayOfWeek) return target
    }
    return firstOrNull { it.dayOfWeek == dayOfWeek }
}

/** 时间表封面（缩略图尺寸，同旧界面）。 */
internal val TimelineEp.thumbnail: String
    get() = cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail)

/** 本次运行中追番 / 取消追番的结果（seasonId → 是否已追）。只在主线程写。 */
internal object SeasonFollowMemory {
    val states = mutableStateMapOf<Int, Boolean>()
}

/**
 * 追番 / 取消追番（与旧电视 SeasonInfoButtons → SeasonViewModel.followSeason 同一个接口：
 * UserRepository.addSeasonFollow / delSeasonFollow）。结果记在 [SeasonFollowMemory]。
 */
@Stable
internal class SeasonFollowActions(
    private val context: Context,
    private val userRepository: UserRepository,
    private val bvUserRepository: BvUserRepository,
    private val scope: CoroutineScope,
) {
    fun isFollowed(seasonId: Int, initial: Boolean = false): Boolean =
        SeasonFollowMemory.states[seasonId] ?: initial

    fun toggle(seasonId: Int, initial: Boolean = false) {
        if (seasonId <= 0) return
        if (!bvUserRepository.isLogin) {
            Toast.makeText(context, R.string.timeline_follow_login, Toast.LENGTH_SHORT).show()
            return
        }
        val target = !isFollowed(seasonId, initial)
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    if (target) userRepository.addSeasonFollow(seasonId = seasonId, preferApiType = Prefs.apiType)
                    else userRepository.delSeasonFollow(seasonId = seasonId, preferApiType = Prefs.apiType)
                }
            }
            val text = result.fold(
                onSuccess = { message ->
                    SeasonFollowMemory.states[seasonId] = target
                    message.ifBlank {
                        context.getString(if (target) R.string.timeline_follow_done else R.string.timeline_unfollow_done)
                    }
                },
                onFailure = { context.getString(R.string.timeline_follow_failed, it.message ?: "") },
            )
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
internal fun rememberSeasonFollow(): SeasonFollowActions {
    val context = LocalContext.current.applicationContext
    val userRepository: UserRepository = koinInject()
    val bvUserRepository: BvUserRepository = koinInject()
    val scope = rememberCoroutineScope()
    return remember(userRepository, bvUserRepository, scope) {
        SeasonFollowActions(context, userRepository, bvUserRepository, scope)
    }
}

/**
 * 时间表的一行（原型 pgcPhone 当日更新列表）：时间 width 44 Bold primary 等宽 | 海报 44×58 圆角 10 |
 * 标题 14sp Bold + Meta「第 N 话」| FilledTonalButton「追番」高 32 内边距 12 12sp（已追显示「已追」）。
 */
@Composable
internal fun TimelineEpisodeRow(
    episode: TimelineEp,
    followed: Boolean,
    onClick: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            episode.publishTime,
            modifier = Modifier.width(44.dp),
            style = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        AsyncImage(
            model = episode.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 44.dp, height = 58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                episode.title,
                style = BvType.Body.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode.publishIndex.isNotBlank()) {
                Text(
                    episode.publishIndex,
                    style = BvType.Meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        BvButton(
            text = stringResource(if (followed) R.string.timeline_followed else R.string.timeline_follow),
            onClick = onFollow,
            style = BvButtonStyle.Tonal,
            height = 32.dp,
            horizontalPadding = 12.dp,
            textStyle = BvType.Label.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
