package dev.aaa1115910.bv.ui.feature.space

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.VideoActionRepository
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvAvatar
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.feature.me.formatCount
import dev.aaa1115910.bv.ui.feature.timeline.SubGridKind
import dev.aaa1115910.bv.ui.feature.timeline.SubPageFrame
import dev.aaa1115910.bv.ui.feature.timeline.SubPageGrid
import dev.aaa1115910.bv.ui.feature.timeline.rememberPendingLoad
import dev.aaa1115910.bv.ui.navigation.SpaceRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.user.UserSpaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * UP 主空间顶部资料（头像、昵称、签名、粉丝 / 关注 / 投稿）与关注按钮。
 * 资料来自 /x/web-interface/card；关注状态与切换走 VideoActionRepository（checkFollowing / followUser / unfollowUser）。
 * 全部在主线程写状态，网络请求切到 IO。
 */
internal class SpaceHeaderHolder(
    private val mid: Long,
    private val repo: VideoActionRepository,
    private val auth: dev.aaa1115910.biliapi.repositories.AuthRepository,
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var face by mutableStateOf("")
        private set
    var sign by mutableStateOf("")
        private set
    var fans by mutableStateOf<Int?>(null)
        private set
    var attention by mutableStateOf<Int?>(null)
        private set
    var archives by mutableStateOf<Int?>(null)
        private set

    /** null：未登录 / 未知（不显示按钮）。 */
    var followed by mutableStateOf<Boolean?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    /** 自己的空间不显示关注按钮。 */
    val canFollow: Boolean get() = repo.isLoggedIn && mid != auth.mid

    init {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    BiliHttpApi.getUserCardInfo(uid = mid, sessData = Prefs.sessData).getResponseData()
                }
            }.onSuccess { data ->
                name = data.card.name
                face = data.card.face
                sign = data.card.sign.trim()
                fans = data.follower
                attention = data.card.attention
                archives = data.archiveCount
            }
        }
        if (canFollow) {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { repo.checkFollowing(mid, Prefs.apiType) }.getOrNull()
                }
                if (result != null) followed = result
            }
        }
    }

    fun toggleFollow(context: Context) {
        val current = followed ?: return
        if (busy) return
        busy = true
        val target = !current
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (target) repo.followUser(mid, Prefs.apiType) else repo.unfollowUser(mid, Prefs.apiType)
                }.getOrDefault(false)
            }
            if (ok) {
                followed = target
                fans = fans?.let { if (target) it + 1 else (it - 1).coerceAtLeast(0) }
            }
            Toast.makeText(
                context,
                when {
                    !ok -> R.string.space_follow_failed
                    target -> R.string.space_follow_done
                    else -> R.string.space_unfollow_done
                },
                Toast.LENGTH_SHORT
            ).show()
            busy = false
        }
    }
}

/**
 * UP 主空间（第 9.11 节模板）：UserSpaceViewModel（加载逻辑同旧电视 UpSpaceScreen / 手机 UserSpaceScreen：
 * 设置 upMid 后 update()，快到底部时再 update()）；网格顶部整行是资料头 + 关注按钮，下面视频网格。
 */
@Composable
fun SpaceScreen(nav: NavController, route: SpaceRoute) {
    val vm: UserSpaceViewModel = koinViewModel()
    val repo: VideoActionRepository = koinInject()
    val auth: dev.aaa1115910.biliapi.repositories.AuthRepository = koinInject()
    val profile: SpaceHeaderHolder = viewModel { SpaceHeaderHolder(route.mid, repo, auth) }
    val context = LocalContext.current
    val pending = rememberPendingLoad()
    val firstRequester = remember { FocusRequester() }

    LaunchedEffect(vm, route.mid) {
        if (vm.upMid != route.mid) {
            vm.upMid = route.mid
            vm.update()
        }
    }
    LaunchedEffect(profile.name) {
        if (profile.name.isNotBlank() && vm.upName.isBlank()) vm.upName = profile.name
    }

    val title = profile.name.ifBlank { vm.upName }.ifBlank { stringResource(R.string.space_title) }
    val videos = vm.tvSpaceVideos
    SubPageFrame(title = title, onBack = { nav.popBackStack() }) { spec ->
        SubPageGrid(
            list = videos,
            grid = spec.grid(SubGridKind.Video),
            // UserSpaceViewModel 的加载中状态是私有的：列表为空且还有下一页时最多按加载中显示一段时间
            loading = videos.isEmpty() && !vm.noMore && pending.waiting,
            noMore = vm.noMore,
            modifier = Modifier.weight(1f),
            onLoadMore = { vm.update() },
            onRetry = {
                pending.retry()
                vm.update()
            },
            emptyMessage = stringResource(R.string.space_empty),
            firstRequester = firstRequester,
            itemKey = { it.avid },
            header = {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    SpaceHeader(holder = profile, onToggleFollow = { profile.toggleFollow(context) })
                }
            },
        ) { _, card, itemModifier ->
            VideoCard(data = card, onClick = { nav.openVideo(card) }, modifier = itemModifier)
        }
    }
}

/** 资料头：Row(spacedBy 16)：头像 72（电视 64）+ Column(昵称 PanelTitle、粉丝 · 关注 · 投稿、签名) + 关注按钮。 */
@Composable
private fun SpaceHeader(holder: SpaceHeaderHolder, onToggleFollow: () -> Unit) {
    val tv = LocalDeviceProfile.current.isTv
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvAvatar(
            url = holder.face.ifBlank { null },
            size = if (tv) 64.dp else 72.dp,
            fallbackText = holder.name,
        )
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (holder.name.isNotBlank()) {
                Text(
                    holder.name,
                    style = BvType.PanelTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val fans = holder.fans
            val attention = holder.attention
            val archives = holder.archives
            if (fans != null && attention != null && archives != null) {
                Text(
                    stringResource(R.string.space_stats, formatCount(fans), formatCount(attention), formatCount(archives)),
                    style = BvType.Meta.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (holder.sign.isNotEmpty()) {
                Text(
                    holder.sign,
                    style = BvType.Small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val followed = holder.followed
        if (holder.canFollow && followed != null) {
            BvButton(
                text = stringResource(if (followed) R.string.space_followed else R.string.space_follow),
                onClick = onToggleFollow,
                style = if (followed) BvButtonStyle.Tonal else BvButtonStyle.Filled,
                icon = if (followed) Icons.Rounded.Check else Icons.Rounded.Add,
                enabled = !holder.busy,
            )
        }
    }
}
