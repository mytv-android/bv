package com.github.mytv.bv.ui.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.component.BvIconButton
import com.github.mytv.bv.ui.designsystem.theme.BvType

/**
 * 平板（原型 setTablet）：Row(padding(20, 24), spacedBy 20)：
 * 左列 width 380（返回 + 「设置」PaneTitle，padding 0×8 下 12；分类列表，选中项 secondaryContainer）|
 * 右栏 weight 1 圆角 28 surfaceContainerLow padding(26, 30) Column(spacedBy 22)：PaneTitle 分类名 + 内容（可滚动）。
 */
@Composable
internal fun SettingsTablet(nav: NavController, initial: SettingsCategory) {
    var selectedId by rememberSaveable { mutableStateOf(initial.id) }
    val selected = SettingsCategory.of(selectedId) ?: SettingsCategory.Appearance
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Row(
        Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            Modifier
                .width(380.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BvIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.ds_back),
                    onClick = { nav.popBackStack() },
                )
                Text(stringResource(R.string.settings_title), style = BvType.PaneTitle, color = scheme.onSurface)
            }
            SettingsCategory.entries.forEach { category ->
                SettingsCategoryItem(
                    category = category,
                    selected = category == selected,
                    onClick = { selectedId = category.id },
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(scheme.surfaceContainerLow, RoundedCornerShape(28.dp)),
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                label = "settingsPane",
            ) { category ->
                SettingsCategoryContent(category = category, nav = nav, paneColor = scheme.surfaceContainerLow) { content ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 30.dp, vertical = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        Text(stringResource(category.title), style = BvType.PaneTitle, color = scheme.onSurface)
                        content()
                    }
                }
            }
        }
    }
}
