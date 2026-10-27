package com.github.mytv.bv.ui.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.focus.BvKey
import com.github.mytv.bv.ui.designsystem.focus.TvColumnPivot
import com.github.mytv.bv.ui.designsystem.focus.onBvKey
import com.github.mytv.bv.ui.designsystem.theme.BvType

/**
 * 电视（原型 setTv）：Row(padding(27, 48), spacedBy 24)：
 * 左列 width 300（「设置」PaneTitle padding 0×12 下 12 + 分类 BvListItem，聚焦即切换右栏，初始焦点在选中项）|
 * 右栏 weight 1 圆角 28 surfaceContainer padding(22, 26) Column(spacedBy 16)：PanelTitle 分类名 + 内容。
 * 左列按 → 进右栏第一项；右栏按 ← 回到左列（列表 focusRestorer 的回退项是当前选中项）；右栏按返回键也回到左列。
 */
@Composable
internal fun SettingsTv(nav: NavController, initial: SettingsCategory) {
    var selectedId by rememberSaveable { mutableStateOf(initial.id) }
    val selected = SettingsCategory.of(selectedId) ?: SettingsCategory.Appearance
    val scheme = MaterialTheme.colorScheme
    val itemRequesters = remember { SettingsCategory.entries.associateWith { FocusRequester() } }
    val paneRequester = remember { FocusRequester() }
    var paneFocused by remember { mutableStateOf(false) }
    var enterRequested by remember { mutableStateOf<SettingsCategory?>(null) }

    // 初始焦点：选中的分类（原型 data-start）
    LaunchedEffect(Unit) {
        withFrameNanos { }
        itemRequesters.getValue(selected).tryFocusEnter()
    }
    BackHandler(enabled = paneFocused) {
        itemRequesters.getValue(selected).tryFocusEnter()
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .padding(horizontal = 48.dp, vertical = 27.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            Modifier
                .width(300.dp)
                .fillMaxHeight(),
        ) {
            Text(
                stringResource(R.string.settings_title),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                style = BvType.PaneTitle,
                color = scheme.onSurface,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                TvColumnPivot {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .focusRestorer(itemRequesters.getValue(selected))
                            .focusGroup()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        SettingsCategory.entries.forEach { category ->
                            SettingsCategoryItem(
                                category = category,
                                selected = category == selected,
                                onClick = {
                                    selectedId = category.id
                                    enterRequested = category
                                },
                                onFocused = { focused ->
                                    if (focused) {
                                        selectedId = category.id
                                        if (enterRequested != category) enterRequested = null
                                    }
                                },
                                modifier = Modifier
                                    .focusRequester(itemRequesters.getValue(category))
                                    // clickable 的焦点目标在触摸模式下不可聚焦；触摸选中分类后
                                    // 按系统返回时，这里仍需能把焦点移回左列。
                                    .focusProperties { canFocus = true }
                                    .onBvKey { key ->
                                        if (key == BvKey.Right) {
                                            enterRequested = category
                                            true
                                        } else false
                                    },
                            )
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(scheme.surfaceContainer, RoundedCornerShape(28.dp)),
        ) {
            key(selected) {
                SettingsCategoryContent(category = selected, nav = nav, paneColor = scheme.surfaceContainer) { content ->
                    LaunchedEffect(enterRequested) {
                        if (enterRequested == selected) {
                            withFrameNanos { }
                            paneRequester.tryFocusEnter()
                            enterRequested = null
                        }
                    }
                    TvColumnPivot {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .onFocusChanged { paneFocused = it.hasFocus }
                                .focusRequester(paneRequester)
                                .focusGroup()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 26.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(stringResource(selected.title), style = BvType.PanelTitle, color = scheme.onSurface)
                            content()
                        }
                    }
                }
            }
        }
    }
}
