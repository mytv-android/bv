package dev.aaa1115910.bv.ui.feature.dynamic

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.entity.user.DynamicType
import dev.aaa1115910.biliapi.entity.user.FollowedUser
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.AvatarShape
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.toComposeShape
import dev.aaa1115910.bv.ui.designsystem.focus.bvFocus
import dev.aaa1115910.bv.ui.designsystem.icon.BvIcons
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.navigation.DynamicDetailRoute
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.CommentViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.user.FollowViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/** 电视顶部标签：全部 / 视频 / 图文。 */
internal enum class DynamicKind(@param:StringRes val label: Int) {
    All(R.string.dynamic_all),
    Video(R.string.dynamic_filter_video),
    Image(R.string.dynamic_filter_image);

    fun matches(item: DynamicItem): Boolean {
        val type = item.effective().type
        return when (this) {
            All -> true
            Video -> type == DynamicType.Av || type == DynamicType.Pgc
            Image -> type == DynamicType.Draw || type == DynamicType.Article || type == DynamicType.Word
        }
    }
}

/** 电视按日期分组：今天 / 昨天 / 更早。 */
internal enum class DynamicDateGroup(@param:StringRes val label: Int) {
    Today(R.string.dynamic_today),
    Yesterday(R.string.dynamic_yesterday),
    Earlier(R.string.dynamic_earlier),
}

/** 转发动态按原动态归类。 */
internal fun DynamicItem.effective(): DynamicItem =
    if (type == DynamicType.Forward) orig ?: this else this

private val TimeOfDayRegex = Regex("^\\d{1,2}:\\d{2}$")

/**
 * 按接口返回的发布时间文字分组（接口给的是「刚刚」「5分钟前」「3小时前」「昨天 21:30」「10月12日」等）。
 */
internal fun DynamicItem.dateGroup(): DynamicDateGroup {
    val time = author.pubTime.trim()
    return when {
        time.startsWith("昨天") -> DynamicDateGroup.Yesterday
        time.isEmpty() || time.contains("刚刚") || time.contains("分钟前") || time.contains("小时前") ||
            time.startsWith("今天") || TimeOfDayRegex.matches(time) -> DynamicDateGroup.Today

        else -> DynamicDateGroup.Earlier
    }
}

/** 卡片封面：视频 / 番剧封面、图文第一张图、直播封面、专栏封面；转发取原动态的。没有时为 null。 */
internal fun DynamicItem.coverUrl(): String? = when (type) {
    DynamicType.Av -> video?.cover
    DynamicType.Pgc -> pgc?.cover
    DynamicType.Draw -> draw?.images?.firstOrNull()?.url
    DynamicType.LiveRcmd -> liveRcmd?.cover
    DynamicType.Article -> article?.covers?.firstOrNull()
    DynamicType.Forward -> orig?.coverUrl()
    DynamicType.Word, DynamicType.None, DynamicType.UgcSeason -> null
}?.takeIf { it.isNotBlank() }

/** 卡片 / 详情的标题文字。 */
internal fun DynamicItem.titleText(): String? = when (type) {
    DynamicType.Av -> video?.title
    DynamicType.Pgc -> pgc?.title
    DynamicType.Draw -> draw?.title?.takeIf { it.isNotBlank() } ?: draw?.text
    DynamicType.Word -> word?.text
    DynamicType.LiveRcmd -> liveRcmd?.title
    DynamicType.Article -> article?.title
    DynamicType.Forward -> word?.text?.takeIf { it.isNotBlank() } ?: orig?.titleText()
    DynamicType.None -> none?.text
    DynamicType.UgcSeason -> null
}?.takeIf { it.isNotBlank() }

/** 「投稿了视频」等；接口带了动作文字时用接口的。 */
@Composable
internal fun DynamicItem.actionText(): String = author.pubAction.takeIf { it.isNotBlank() } ?: stringResource(
    when (type) {
        DynamicType.Av -> R.string.dynamic_action_video
        DynamicType.Forward -> R.string.dynamic_action_forward
        DynamicType.Article -> R.string.dynamic_action_article
        DynamicType.LiveRcmd -> R.string.dynamic_action_live
        DynamicType.Pgc -> R.string.dynamic_action_pgc
        else -> R.string.dynamic_action_dynamic
    }
)

/** 「2 小时前 · 投稿了视频」。 */
@Composable
internal fun DynamicItem.metaText(): String {
    val action = actionText()
    val time = author.pubTime.trim()
    return if (time.isEmpty()) action else stringResource(R.string.dynamic_meta, time, action)
}

/** 数字缩写：1.2万 / 3.4亿。 */
@Composable
internal fun formatCount(count: Int): String = when {
    count >= 100_000_000 -> stringResource(R.string.dynamic_count_yi, trimDecimal(count / 100_000_000f))
    count >= 10_000 -> stringResource(R.string.dynamic_count_wan, trimDecimal(count / 10_000f))
    else -> count.coerceAtLeast(0).toString()
}

private fun trimDecimal(value: Float): String = String.format(Locale.US, "%.1f", value).removeSuffix(".0")

/** 「1.2万播放」：接口给的已经是带单位的文字（App 接口可能已带「观看」），以数字 / 万 / 亿结尾时才补后缀。 */
@Composable
internal fun statText(raw: String, @StringRes format: Int): String? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    val last = text.last()
    return if (last.isDigit() || last == '万' || last == '亿') stringResource(format, text) else text
}

/**
 * 打开一条动态（旧手机 DynamicScreen.onClickDynamicItem）：视频进视频详情（番剧稿件进番剧详情），
 * 番剧进番剧详情，其他进动态详情；没有 id 时提示「原动态不存在」。
 */
internal fun openDynamic(nav: NavController, context: Context, item: DynamicItem) {
    when (item.type) {
        DynamicType.Av -> item.video?.let { video ->
            val seasonId = video.seasonId?.takeIf { it > 0 }
            if (seasonId != null) {
                nav.navigate(SeasonRoute(seasonId = seasonId, epId = video.epid?.takeIf { it > 0 }))
            } else {
                nav.openVideo(video.aid)
            }
        }

        DynamicType.Pgc -> item.pgc?.let { pgc ->
            val seasonId = pgc.seasonId.takeIf { it > 0 }
            val epId = pgc.epid.takeIf { it > 0 }
            if (seasonId != null || epId != null) nav.navigate(SeasonRoute(seasonId = seasonId, epId = epId))
        }

        else -> {
            val id = item.id
            if (!id.isNullOrBlank()) {
                nav.navigate(DynamicDetailRoute(id))
            } else {
                Toast.makeText(context, R.string.dynamic_source_missing, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * 把评论区切换到 [item]：等正在进行的加载结束后再刷新。不取消进行中的请求——
 * CommentViewModel.loadMoreComment 被取消时 updatingComments 会停在 true。
 */
internal fun CommentViewModel.showCommentsOf(item: DynamicItem): Job = viewModelScope.launch {
    snapshotFlow { updatingComments }.first { !it }
    commentId = item.commentId
    commentType = item.commentType
    if (item.commentId == 0L) {
        comments.clear()
        hasMoreComments = false
    } else {
        viewModelScope.launch { refreshComments() }
    }
}

/** 评论区加载更多（列表快到底部时调用）。 */
internal fun CommentViewModel.loadMoreCommentsIfNeeded() {
    if (commentId == 0L || !hasMoreComments || updatingComments || refreshingComments) return
    viewModelScope.launch { loadMoreComment() }
}

/**
 * 动态页三端共享的状态（ViewModel 在 DynamicScreen 里创建）。
 * 加载调用照抄旧手机 DynamicScreen（loadMoreAll / clearAllData），但在 viewModelScope（主线程）上调用：
 * DynamicViewModel 会直接写 SnapshotStateList。
 */
@Stable
internal class DynamicPageState(
    val vm: DynamicViewModel,
    val follow: FollowViewModel,
    val commentVm: CommentViewModel,
) {
    /** 只看某个 UP 主（null = 全部）。接口不支持按 UP 主取动态，在已加载的动态里筛选。 */
    var upFilter by mutableStateOf<Long?>(null)
        private set

    /** 电视标签：全部 / 视频 / 图文。 */
    var kind by mutableStateOf(DynamicKind.All)

    /** 平板右栏显示的动态。 */
    var selected by mutableStateOf<DynamicItem?>(null)
        private set

    var pulling by mutableStateOf(false)
        private set

    private var autoLoads = 0
    private var commentJob: Job? = null

    val isLogin: Boolean get() = vm.isLogin
    val loading: Boolean get() = vm.loadingAll

    /** 头像行：关注列表前 20 个（FollowViewModel 的顺序）。 */
    val ups: List<FollowedUser> by derivedStateOf { follow.followedUsers.take(20) }

    val items: List<DynamicItem> by derivedStateOf {
        val up = upFilter
        val currentKind = kind
        vm.dynamicAllList.filter { (up == null || it.author.mid == up) && currentKind.matches(it) }
    }

    fun selectUp(mid: Long?) {
        upFilter = mid
        autoLoads = 0
    }

    fun isSelected(item: DynamicItem): Boolean {
        val current = selected ?: return false
        return current === item || (current.id != null && current.id == item.id)
    }

    fun select(item: DynamicItem) {
        if (isSelected(item)) return
        selected = item
        commentJob?.cancel()
        commentJob = commentVm.showCommentsOf(item)
    }

    fun ensureLoaded() {
        if (vm.isLogin && vm.dynamicAllList.isEmpty() && !vm.loadingAll) loadMore()
    }

    fun loadMore() {
        if (vm.loadingAll || !vm.allHasMore || !vm.isLogin) return
        vm.viewModelScope.launch { vm.loadMoreAll() }
    }

    fun refresh() {
        if (vm.loadingAll) return
        pulling = true
        vm.viewModelScope.launch {
            try {
                vm.clearAllData()
                vm.loadMoreAll()
            } finally {
                pulling = false
            }
        }
    }

    /** 筛选（UP 主 / 类型）后结果太少时自动再加载几页（最多 5 页）。 */
    fun autoFill() {
        if (upFilter == null && kind == DynamicKind.All) return
        if (items.size >= 6 || autoLoads >= 5 || vm.loadingAll || !vm.allHasMore) return
        autoLoads++
        loadMore()
    }
}

/**
 * UP 主头像行（原型 upRow）：每项 Column(spacedBy 4, 11sp)：BvAvatar(48，奇数项 Sunny) + 名字；
 * 第一项「全部」；选中的筛选项外圈 2dp primary。点头像 = 只看该 UP 主（再点一次取消）。
 */
@Composable
internal fun DynamicUpRow(
    ups: List<FollowedUser>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "all", contentType = "up") {
            UpAvatarItem(
                name = stringResource(R.string.dynamic_all),
                avatar = null,
                shape = AvatarShape.Cookie,
                selected = selected == null,
                onClick = { onSelect(null) },
                isAll = true,
            )
        }
        itemsIndexed(ups, key = { _, up -> up.mid }, contentType = { _, _ -> "up" }) { index, up ->
            UpAvatarItem(
                name = up.name,
                avatar = up.avatar,
                shape = if ((index + 1) % 2 == 1) AvatarShape.Sunny else AvatarShape.Cookie,
                selected = selected == up.mid,
                onClick = { onSelect(if (selected == up.mid) null else up.mid) },
            )
        }
    }
}

@Composable
private fun UpAvatarItem(
    name: String,
    avatar: String?,
    shape: AvatarShape,
    selected: Boolean,
    onClick: () -> Unit,
    isAll: Boolean = false,
) {
    val composeShape = shape.toComposeShape()
    val ring = MaterialTheme.colorScheme.primary
    Column(
        Modifier.widthIn(max = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .drawBehind {
                    // box-shadow: 0 0 0 2px primary —— 在头像形状外扩 2dp 画一圈
                    if (selected) {
                        val w = 2.dp.toPx()
                        val outline = composeShape.createOutline(
                            Size(size.width + w * 2, size.height + w * 2),
                            layoutDirection,
                            this,
                        )
                        translate(-w, -w) { drawOutline(outline, ring) }
                    }
                }
                .bvFocus(corner = 24.dp)
                .clip(composeShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isAll) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        BvIcons.Dynamic,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else {
                BvAvatar(url = avatar, size = 48.dp, fallbackText = name, shape = shape)
            }
        }
        Text(
            name,
            style = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** 未登录：居中提示 + 「登录」按钮。 */
@Composable
internal fun DynamicLoginPrompt(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    buttonRequester: FocusRequester? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.dynamic_login_required),
                style = BvType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BvButton(
                text = stringResource(R.string.dynamic_login),
                onClick = onLogin,
                modifier = if (buttonRequester != null) Modifier.focusRequester(buttonRequester) else Modifier,
            )
        }
    }
}
