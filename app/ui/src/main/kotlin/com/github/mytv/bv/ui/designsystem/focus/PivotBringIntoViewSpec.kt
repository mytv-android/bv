@file:OptIn(ExperimentalFoundationApi::class)

package com.github.mytv.bv.ui.designsystem.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile

/** 焦点项保持在容器 pivot 比例处：横向行 0.15，纵向列表 0.30。 */
class PivotBringIntoViewSpec(private val pivot: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - containerSize * pivot
}

private val RowPivot = PivotBringIntoViewSpec(0.15f)
private val ColumnPivot = PivotBringIntoViewSpec(0.30f)

/** 只在电视上生效。LazyRow 外层套 TvRowPivot，LazyColumn 外层套 TvColumnPivot。 */
@Composable
fun TvRowPivot(content: @Composable () -> Unit) {
    if (LocalDeviceProfile.current.isTv) CompositionLocalProvider(LocalBringIntoViewSpec provides RowPivot, content = content) else content()
}

@Composable
fun TvColumnPivot(content: @Composable () -> Unit) {
    if (LocalDeviceProfile.current.isTv) CompositionLocalProvider(LocalBringIntoViewSpec provides ColumnPivot, content = content) else content()
}
