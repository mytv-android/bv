package com.github.mytv.bv.ui.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.adaptive.DeviceLayout
import com.github.mytv.bv.ui.designsystem.adaptive.LocalDeviceProfile
import com.github.mytv.bv.ui.designsystem.component.BvIconButton
import com.github.mytv.bv.ui.designsystem.theme.BvType

/** 请求焦点（组节点会把焦点交给第一个子项）；失败时不抛异常。 */
internal fun FocusRequester.tryToolFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)

/** 模板给内容区的内边距与间距（手机 16 / 12，平板 24 / 16，电视 48 / 18）。 */
internal class ToolPageInsets(val padding: PaddingValues, val spacing: Dp)

/**
 * 第 9.11 节「原型里没有画的页面」统一模板：
 * - 手机：LargeFlexibleTopAppBar（滚动收起为单行，标题 = 页面名）+ 内容，水平内边距 16，间距 12；
 * - 平板：左上返回按钮 + PageTitleLarge 标题（padding 20 / 24）+ 内容，间距 16；
 * - 电视：PageTitle 标题 at (48, 27)；内容 contentPadding(start 48, end 48, top 80, bottom 27)，间距 18。
 * content 需要自己滚动（LazyColumn / verticalScroll），并使用传入的 [ToolPageInsets]。
 */
@Composable
internal fun ToolPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (ToolPageInsets) -> Unit,
) {
    when (LocalDeviceProfile.current.layout) {
        DeviceLayout.Phone -> PhoneToolPage(title, onBack, actions, content)
        DeviceLayout.Tablet -> TabletToolPage(title, onBack, actions, content)
        DeviceLayout.Tv -> TvToolPage(title, actions, content)
    }
}

@Composable
private fun PhoneToolPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (ToolPageInsets) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    BvIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.ds_back),
                        onClick = onBack,
                    )
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        content(
            ToolPageInsets(
                padding = PaddingValues(
                    start = inner.calculateStartPadding(layoutDirection) + 16.dp,
                    end = inner.calculateEndPadding(layoutDirection) + 16.dp,
                    top = inner.calculateTopPadding() + 8.dp,
                    bottom = inner.calculateBottomPadding() + 24.dp,
                ),
                spacing = 12.dp,
            )
        )
    }
}

@Composable
private fun TabletToolPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (ToolPageInsets) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.ds_back),
                onClick = onBack,
            )
            Text(title, style = BvType.PageTitleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            actions()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            content(
                ToolPageInsets(
                    padding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                    spacing = 16.dp,
                )
            )
        }
    }
}

@Composable
private fun TvToolPage(
    title: String,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (ToolPageInsets) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, top = 27.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = BvType.PageTitle, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            actions()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // 标题行 27 + 32 = 59；内容从 80 开始 → 再留 21
            content(
                ToolPageInsets(
                    padding = PaddingValues(start = 48.dp, end = 48.dp, top = 21.dp, bottom = 27.dp),
                    spacing = 18.dp,
                )
            )
        }
    }
}
