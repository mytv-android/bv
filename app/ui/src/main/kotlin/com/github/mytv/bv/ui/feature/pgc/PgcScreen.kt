package com.github.mytv.bv.ui.feature.pgc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.feature.timeline.SeasonFollowActions
import com.github.mytv.bv.ui.feature.timeline.rememberSeasonFollow
import org.koin.compose.koinInject

/** 影视页三端共享的状态：当前标签、该分类的数据、页面模型（时间表 / 精选补充信息）、追番操作。 */
internal class PgcUiState(
    val tab: Int,
    val type: PgcType,
    val feed: PgcFeed,
    val model: PgcPageModel,
    val follow: SeasonFollowActions,
    val onTab: (Int) -> Unit,
)

/**
 * 影视（第 9.6 节，原型 pgcPhone / pgcTablet / pgcTv）。
 * 数据：Pgc*ViewModel（旧电视 PgcContent / PgcScaffold：轮播 + 推荐 feed）、SeasonRepository.getTimeline（时间表）。
 */
@Composable
fun PgcScreen(nav: NavController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val type = PgcTabs[tab]
    val vm = pgcViewModel(type)
    val feed = rememberPgcFeed(vm)
    val seasonRepository: SeasonRepository = koinInject()
    val videoDetailRepository: VideoDetailRepository = koinInject()
    val model: PgcPageModel = viewModel { PgcPageModel(seasonRepository, videoDetailRepository) }
    val follow = rememberSeasonFollow()

    val filter = type.timelineFilter
    LaunchedEffect(filter) { filter?.let(model::ensureTimeline) }

    val state = PgcUiState(
        tab = tab,
        type = type,
        feed = feed,
        model = model,
        follow = follow,
        onTab = { tab = it },
    )
    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> PgcPhone(nav, state)
        DeviceLayout.Tablet -> PgcTablet(nav, state)
        DeviceLayout.Tv -> PgcTv(nav, state)
    }
}
