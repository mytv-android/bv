package dev.aaa1115910.bv.ui.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvSearchField

/**
 * 平板搜索（原型 searchTablet）：外壳在左侧显示 TabletRail（不选中任何项）；
 * Column(padding(start 8, end 28, top 20), spacedBy 18)：
 * Row(spacedBy 16)：搜索框（宽 560，surfaceContainerHighest）+ 分类标签；下面是结果网格。
 * 未提交时在搜索框下显示联想 / 历史 + 热搜（宽 560）。
 */
@Composable
internal fun SearchTablet(nav: NavController, state: SearchUiState, follow: FollowState) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val openers = rememberSearchOpeners(nav, state)
    val keyword = state.input.keyword
    val hasResults = state.submitted.isNotEmpty()
    val showResults = !state.editing && hasResults
    var showFilter by remember { mutableStateOf(false) }

    val submit: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            keyboard?.hide()
            state.search(query)
        }
    }
    BackHandler(enabled = state.editing && hasResults) {
        focusManager.clearFocus()
        keyboard?.hide()
        state.input.keyword = state.submitted
        state.editing = false
    }
    SuggestEffect(state)

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 8.dp, end = 28.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvSearchField(
                value = keyword,
                placeholder = stringResource(R.string.search_placeholder),
                modifier = Modifier
                    .width(560.dp)
                    .onFocusChanged { if (it.hasFocus) state.editing = true },
                onValueChange = state::onInput,
                onSearch = submit,
                onVoiceResult = submit,
                active = true,
                autoFocus = !hasResults,
            )
            if (showResults) {
                SearchCategoryChips(selected = state.category, onSelect = { state.category = it })
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
        ) {
            when {
                showResults -> SearchResultsPane(
                    state = state,
                    follow = follow,
                    openers = openers,
                    tv = false,
                    onFilter = { showFilter = true },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp + navBottom),
                )

                keyword.isNotBlank() -> SuggestionList(
                    state = state,
                    onPick = submit,
                    modifier = Modifier
                        .width(560.dp)
                        .fillMaxHeight(),
                )

                else -> SearchIdleContent(
                    state = state,
                    onPick = submit,
                    modifier = Modifier
                        .width(560.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
    if (showFilter) SearchFilterDialog(state, onDismiss = { showFilter = false })
}
