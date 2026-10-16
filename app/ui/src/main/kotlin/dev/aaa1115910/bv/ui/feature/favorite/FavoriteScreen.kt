package dev.aaa1115910.bv.ui.feature.favorite

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.designsystem.component.BvChip
import dev.aaa1115910.bv.ui.designsystem.component.VideoCard
import dev.aaa1115910.bv.ui.feature.timeline.SubGridKind
import dev.aaa1115910.bv.ui.feature.timeline.SubGridSpec
import dev.aaa1115910.bv.ui.feature.timeline.SubPageFrame
import dev.aaa1115910.bv.ui.feature.timeline.SubPageGrid
import dev.aaa1115910.bv.ui.feature.timeline.SubPageLoginPrompt
import dev.aaa1115910.bv.ui.navigation.LoginRoute
import dev.aaa1115910.bv.ui.navigation.openVideo
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository

/**
 * 收藏夹（第 9.11 节模板）：FavoriteViewModel（创建时加载收藏夹列表和第一个收藏夹）；
 * 顶部收藏夹 BvChip 切换（同旧电视 FavoriteScreen：电视上聚焦即切换），下方视频网格。
 */
@Composable
fun FavoriteScreen(nav: NavController) {
    val vm: FavoriteViewModel = koinViewModel()
    val userRepository: BvUserRepository = koinInject()
    val firstRequester = remember { FocusRequester() }
    SubPageFrame(title = stringResource(R.string.favorite_title), onBack = { nav.popBackStack() }) { spec ->
        if (!userRepository.isLogin) {
            SubPageLoginPrompt(onLogin = { nav.navigate(LoginRoute) }, tv = spec.tv)
        } else {
            FavoriteContent(
                vm = vm,
                grid = spec.grid(SubGridKind.Video),
                chipsPadding = PaddingValues(start = spec.horizontal, end = spec.horizontal, top = 4.dp, bottom = 6.dp),
                onOpen = { nav.openVideo(it) },
                modifier = Modifier.weight(1f),
                firstRequester = firstRequester,
            )
        }
    }
}

/** 切换收藏夹：同旧电视 FavoriteScreen.updateCurrentFavoriteFolder。 */
internal fun FavoriteViewModel.selectFolder(folder: FavoriteFolderMetadata) {
    if (currentFavoriteFolderMetadata?.id == folder.id) return
    currentFavoriteFolderMetadata = folder
    favorites.clear()
    resetPageNumber()
    updateFolderItems(force = true)
}

/** 收藏夹标签行 + 视频网格（「我的」平板右栏也用）。 */
@Composable
internal fun FavoriteContent(
    vm: FavoriteViewModel,
    grid: SubGridSpec,
    chipsPadding: PaddingValues,
    onOpen: (VideoCardData) -> Unit,
    modifier: Modifier = Modifier,
    firstRequester: FocusRequester? = null,
) {
    val folders = vm.favoriteFolderMetadataList
    val current = vm.currentFavoriteFolderMetadata
    Column(modifier) {
        if (folders.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (grid.tv) Modifier.focusRestorer().focusGroup() else Modifier),
                contentPadding = chipsPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(folders) { folder ->
                    BvChip(
                        text = folder.title,
                        selected = current?.id == folder.id,
                        onClick = { vm.selectFolder(folder) },
                        selectOnFocus = grid.tv,
                    )
                }
            }
        }
        val loading = vm.updatingFolders || vm.updatingFolderItems
        SubPageGrid(
            list = vm.favorites,
            grid = grid,
            loading = loading,
            noMore = !vm.updatingFolderItems,
            modifier = Modifier.weight(1f),
            onLoadMore = { vm.updateFolderItems() },
            onRetry = if (current != null) {
                {
                    vm.favorites.clear()
                    vm.updateFolderItems(force = true)
                }
            } else null,
            emptyMessage = stringResource(
                if (folders.isEmpty()) R.string.favorite_no_folder else R.string.favorite_empty
            ),
            firstRequester = firstRequester,
        ) { _, card, itemModifier ->
            VideoCard(data = card, onClick = { onOpen(card) }, modifier = itemModifier)
        }
    }
}
