package dev.aaa1115910.bv.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.theme.BvType
import dev.aaa1115910.bv.ui.navigation.SettingsRoute

/**
 * 手机列表页（原型 setPhone）：标题「设置」PageTitle(padding top=statusBar+6, start 20, bottom 12)
 * + Column(padding 水平 8, spacedBy 2) 8 个分类；点分类 → SettingsRoute(category)。
 */
@Composable
internal fun SettingsPhoneList(nav: NavController) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Text(
            stringResource(R.string.settings_title),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
            style = BvType.PageTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsCategory.entries.forEach { category ->
                SettingsCategoryItem(
                    category = category,
                    selected = false,
                    onClick = { nav.navigate(SettingsRoute(category.id)) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 手机分类详情页：LargeFlexibleTopAppBar（滚动收起为单行，标题 = 分类名）+ 内容（水平 16，spacedBy 22）。
 */
@Composable
internal fun SettingsPhoneDetail(nav: NavController, category: SettingsCategory) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val surface = MaterialTheme.colorScheme.surface
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(category.title)) },
                navigationIcon = {
                    BvIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.ds_back),
                        onClick = { nav.popBackStack() },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        SettingsCategoryContent(category = category, nav = nav, paneColor = surface) { content ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                content()
            }
        }
    }
}
