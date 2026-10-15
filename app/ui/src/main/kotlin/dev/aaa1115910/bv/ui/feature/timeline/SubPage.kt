package dev.aaa1115910.bv.ui.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.theme.BvType

/*
 * 第 9.11 节「原型里没有画的页面」统一模板（新番时间表、番剧索引、动态详情共用）：
 * - 手机：LargeFlexibleTopAppBar（滚动收起为单行，标题 = 页面名）+ 内容，水平内边距 16，间距 12；
 * - 平板：左上返回按钮 + BvType.PageTitleLarge 标题（padding 20/24）+ 网格，间距 16；
 * - 电视：标题 BvType.PageTitle at (48, 27)；内容网格 contentPadding(start 48, end 48, bottom 27)，间距 18。
 */

/** 手机模板。content 的参数是列表底部应留出的内边距（系统导航栏 + 16）。 */
@Composable
internal fun SubPagePhone(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (bottomPadding: Dp) -> Unit,
) {
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(behavior.nestedScrollConnection)
    ) {
        LargeFlexibleTopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                BvIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.ds_back),
                    onClick = onBack,
                    modifier = Modifier.padding(start = 4.dp),
                )
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            scrollBehavior = behavior,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content(bottom)
        }
    }
}

/** 平板模板的标题行：返回按钮 + PageTitleLarge 标题 + 右侧可选按钮。 */
@Composable
internal fun SubPageTabletHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BvIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.ds_back),
            onClick = onBack,
        )
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = BvType.PageTitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing()
    }
}

/** 电视模板的标题行：标题 at (48, 27)，右侧可放标签等。 */
@Composable
internal fun SubPageTvTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 48.dp, top = 27.dp, end = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = BvType.PageTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        trailing()
    }
}

/** requestFocus 不抛异常且成功时返回 true（焦点目标可能还没布局好）。 */
internal fun FocusRequester.tryRequestFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)
