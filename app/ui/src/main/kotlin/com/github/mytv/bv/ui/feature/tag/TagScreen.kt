package com.github.mytv.bv.ui.feature.tag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.http.BiliHttpApi
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.VideoCard
import com.github.mytv.bv.ui.feature.timeline.SubGridKind
import com.github.mytv.bv.ui.feature.timeline.SubPageFrame
import com.github.mytv.bv.ui.feature.timeline.SubPageGrid
import com.github.mytv.bv.ui.feature.timeline.rememberPendingLoad
import com.github.mytv.bv.ui.navigation.TagRoute
import com.github.mytv.bv.ui.navigation.openVideo
import com.github.mytv.bv.viewmodel.TagViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

/** 路由里只有 tagId：标签名用 /x/tag/detail 取（旧界面由 Intent 传入）。主线程写状态。 */
internal class TagNameHolder(tagId: Int) : ViewModel() {
    var name by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    BiliHttpApi.getTagDetail(tagId = tagId, pageNumber = 1, pageSize = 1).getResponseData().info.tagName
                }
            }.onSuccess { name = it.trim() }
        }
    }
}

/**
 * Tag 页（第 9.11 节模板）：TagViewModel（加载逻辑同旧电视 TagScreen：设置 tagId 后 update()，
 * 快到底部时再 update()），视频网格。
 */
@Composable
fun TagScreen(nav: NavController, route: TagRoute) {
    val vm: TagViewModel = koinViewModel()
    val nameHolder: TagNameHolder = viewModel { TagNameHolder(route.tagId) }
    val pending = rememberPendingLoad()
    val firstRequester = remember { FocusRequester() }

    LaunchedEffect(vm, route.tagId) {
        if (vm.tagId != route.tagId) {
            vm.tagId = route.tagId
            vm.update()
        }
    }
    LaunchedEffect(nameHolder.name) {
        if (nameHolder.name.isNotBlank()) vm.tagName = nameHolder.name
    }

    val title = vm.tagName.ifBlank { stringResource(R.string.tag_title) }
    val videos = vm.topVideos
    SubPageFrame(title = title, onBack = { nav.popBackStack() }) { spec ->
        SubPageGrid(
            list = videos,
            grid = spec.grid(SubGridKind.Video),
            // TagViewModel 的加载中状态是私有的：列表为空且没加载完时最多按加载中显示一段时间
            loading = videos.isEmpty() && !vm.noMore && pending.waiting,
            noMore = vm.noMore,
            modifier = Modifier.weight(1f),
            onLoadMore = { vm.update() },
            onRetry = {
                pending.retry()
                vm.update()
            },
            emptyMessage = stringResource(R.string.tag_empty),
            firstRequester = firstRequester,
            itemKey = { it.avid },
        ) { _, card, itemModifier ->
            VideoCard(data = card, onClick = { nav.openVideo(card) }, modifier = itemModifier)
        }
    }
}
