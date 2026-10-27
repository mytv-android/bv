package com.github.mytv.bv.ui.navigation

import androidx.navigation.NavController
import com.github.mytv.bv.entity.carddata.VideoCardData

/** 切换一级页面：保存/恢复各一级页面的状态。 */
fun NavController.navigateTopLevel(top: TopLevel) {
    navigate(top.route) {
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 打开视频卡：PGC 内容（jumpToSeason）进番剧详情，其余进视频详情。 */
fun NavController.openVideo(data: VideoCardData) {
    if (data.jumpToSeason && data.epId != null) {
        navigate(SeasonRoute(epId = data.epId))
    } else {
        navigate(VideoRoute(data.avid))
    }
}

fun NavController.openVideo(aid: Long) = navigate(VideoRoute(aid))
