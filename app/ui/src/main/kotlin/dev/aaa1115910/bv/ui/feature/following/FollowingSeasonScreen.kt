package dev.aaa1115910.bv.ui.feature.following

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.season.FollowingSeason
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonStatus
import dev.aaa1115910.biliapi.entity.season.FollowingSeasonType
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.adaptive.BvInputMode
import dev.aaa1115910.bv.ui.designsystem.adaptive.DeviceLayout
import dev.aaa1115910.bv.ui.designsystem.adaptive.LocalDeviceProfile
import dev.aaa1115910.bv.ui.designsystem.component.BvButton
import dev.aaa1115910.bv.ui.designsystem.component.BvButtonStyle
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.PosterCard
import dev.aaa1115910.bv.ui.feature.timeline.DeleteConfirmDialog
import dev.aaa1115910.bv.ui.feature.timeline.DeleteModeHint
import dev.aaa1115910.bv.ui.feature.timeline.SubGridKind
import dev.aaa1115910.bv.ui.feature.timeline.SubGridSpec
import dev.aaa1115910.bv.ui.feature.timeline.SubPageFrame
import dev.aaa1115910.bv.ui.feature.timeline.SubPageGrid
import dev.aaa1115910.bv.ui.feature.timeline.SubPageLoginPrompt
import dev.aaa1115910.bv.ui.feature.timeline.deleteModeKeys
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.SeasonRoute
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.getDisplayName
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.viewmodel.user.FollowingSeasonViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository

/**
 * 追番追剧（第 9.11 节模板）：FollowingSeasonViewModel + 旧电视 FollowingSeasonFilter 的类型 / 状态筛选
 * （这里直接做成两组 BvChip），海报网格（手机 3 列、平板 Adaptive(120)、电视 Fixed(6)）→ SeasonRoute。
 * 删除模式：触屏长按海报进入，电视菜单键 / 长按确认键进入；点击确认取消追番，长按直接取消，返回键退出。
 */
@Composable
fun FollowingSeasonScreen(nav: NavController) {
    val vm: FollowingSeasonViewModel = koinViewModel()
    val userRepository: BvUserRepository = koinInject()
    val firstRequester = remember { FocusRequester() }
    SubPageFrame(title = stringResource(R.string.following_seasons_title), onBack = { nav.popBackStack() }) { spec ->
        if (!userRepository.isLogin) {
            SubPageLoginPrompt(onLogin = { nav.navigate(LoginRoute) }, tv = spec.tv)
        } else {
            FollowingSeasonContent(
                vm = vm,
                grid = spec.grid(SubGridKind.Poster),
                chipsPadding = PaddingValues(start = spec.horizontal, end = spec.horizontal, top = 4.dp, bottom = 6.dp),
                onOpen = { nav.navigate(SeasonRoute(seasonId = it)) },
                modifier = Modifier.weight(1f),
                firstRequester = firstRequester,
            )
        }
    }
}

/** 切换筛选：同旧电视 FollowingSeasonScreen（条件变化 → clearData + loadMore）。 */
private fun FollowingSeasonViewModel.applyFilter(type: FollowingSeasonType, status: FollowingSeasonStatus) {
    if (followingSeasonType == type && followingSeasonStatus == status) return
    followingSeasonType = type
    followingSeasonStatus = status
    clearData()
    loadMore()
}

/** 类型 / 状态标签 + 海报网格（「我的」平板右栏也用）。onOpen 参数是 seasonId。 */
@Composable
internal fun FollowingSeasonContent(
    vm: FollowingSeasonViewModel,
    grid: SubGridSpec,
    chipsPadding: PaddingValues,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    firstRequester: FocusRequester? = null,
) {
    val context = LocalContext.current
    val profile = LocalDeviceProfile.current
    val hintPadding = when (profile.layout) {
        DeviceLayout.Phone -> 16.dp
        DeviceLayout.Tablet -> 24.dp
        DeviceLayout.Tv -> 48.dp
    }

    var deleteMode by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FollowingSeason?>(null) }
    var focusedSeason by remember { mutableStateOf<FollowingSeason?>(null) }

    BackHandler(enabled = deleteMode) { deleteMode = false }

    LaunchedEffect(deleteMode, vm.followingSeasons.size, vm.deleting) {
        if (deleteMode && !vm.deleting && vm.followingSeasons.isEmpty()) deleteMode = false
    }

    LaunchedEffect(vm) {
        if (vm.followingSeasons.isEmpty() && !vm.updating && !vm.noMore) vm.loadMore()
    }
    Column(
        modifier
            .fillMaxSize()
            .deleteModeKeys(
                deleteMode = deleteMode,
                onEnter = { deleteMode = true },
                onExit = { deleteMode = false },
                onDirectDelete = {
                    focusedSeason?.let { vm.unfollowSeason(it.seasonId) }
                    focusedSeason = null
                },
            )
    ) {
        DeleteModeHint(
            deleteMode = deleteMode,
            clearSupported = false,
            dpad = profile.input == BvInputMode.DPad,
            modifier = Modifier.padding(horizontal = hintPadding),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(chipsPadding)
                .then(if (grid.tv) Modifier.focusRestorer().focusGroup() else Modifier),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FollowingSeasonType.entries.forEach { type ->
                BvChip(
                    text = type.getDisplayName(context),
                    selected = vm.followingSeasonType == type,
                    onClick = { vm.applyFilter(type, vm.followingSeasonStatus) },
                )
            }
            Spacer(Modifier.width(8.dp))
            FollowingSeasonStatus.entries.forEach { status ->
                BvChip(
                    text = status.getDisplayName(context),
                    selected = vm.followingSeasonStatus == status,
                    onClick = { vm.applyFilter(vm.followingSeasonType, status) },
                )
            }
        }
        SubPageGrid(
            list = vm.followingSeasons,
            grid = grid,
            loading = vm.updating,
            noMore = vm.noMore,
            modifier = Modifier.weight(1f),
            onLoadMore = { if (!vm.noMore && !vm.updating) vm.loadMore() },
            onRetry = { vm.loadMore() },
            emptyMessage = stringResource(R.string.following_seasons_empty),
            firstRequester = firstRequester,
            itemKey = { it.seasonId },
        ) { _, season, itemModifier ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PosterCard(
                    title = season.title,
                    cover = season.cover.takeIf { it.isNotBlank() }
                        ?.resizedImageUrl(ImageSize.SeasonCoverThumbnail) ?: "",
                    onClick = {
                        if (deleteMode) deleteTarget = season else onOpen(season.seasonId)
                    },
                    modifier = itemModifier.onFocusChanged {
                        if (it.isFocused) {
                            focusedSeason = season
                        } else if (focusedSeason?.seasonId == season.seasonId) {
                            focusedSeason = null
                        }
                    },
                    onLongClick = {
                        if (deleteMode) vm.unfollowSeason(season.seasonId) else deleteMode = true
                    },
                )
                if (deleteMode) {
                    BvButton(
                        text = stringResource(R.string.ds_delete),
                        icon = Icons.Default.Delete,
                        onClick = { deleteTarget = season },
                        style = BvButtonStyle.Filled,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        height = 32.dp,
                        horizontalPadding = 12.dp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedSeason = season
                                } else if (focusedSeason?.seasonId == season.seasonId) {
                                    focusedSeason = null
                                }
                            },
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            title = stringResource(R.string.following_season_delete_confirm_title),
            text = stringResource(R.string.following_season_delete_confirm_text, target.title),
            onConfirm = {
                vm.unfollowSeason(target.seasonId)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}
