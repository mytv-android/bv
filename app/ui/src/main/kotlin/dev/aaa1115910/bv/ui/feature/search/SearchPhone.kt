package dev.aaa1115910.bv.ui.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import dev.aaa1115910.bv.appui.R
import dev.aaa1115910.bv.ui.designsystem.component.BvIconButton
import dev.aaa1115910.bv.ui.designsystem.component.BvSearchField

/**
 * 手机搜索（原型 searchPhone）：
 * 顶栏 Row(padding(top = statusBar + 6, start 12, end 12, bottom 8), spacedBy 6)：返回 + 搜索框（自动弹键盘）；
 * 有输入 → 联想；无输入 → 历史 + 热搜；提交后 → 分类标签行 + 结果列表。
 */
@Composable
internal fun SearchPhone(nav: NavController, state: SearchUiState, follow: FollowState) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val openers = rememberSearchOpeners(nav, state)
    val keyword = state.input.keyword
    val hasResults = state.submitted.isNotEmpty()
    val showResults = !state.editing && hasResults

    val submit: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            keyboard?.hide()
            state.search(query)
        }
    }
    // 在结果页重新编辑时，返回键回到结果；否则离开搜索页
    val closeEditing: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        state.input.keyword = state.submitted
        state.editing = false
    }
    val back: () -> Unit = {
        if (state.editing && hasResults) {
            closeEditing()
        } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            nav.popBackStack()
        }
    }
    BackHandler(enabled = state.editing && hasResults) { closeEditing() }
    SuggestEffect(state)

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BvIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.search_back),
                onClick = back,
            )
            BvSearchField(
                value = keyword,
                placeholder = stringResource(R.string.search_placeholder),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { if (it.hasFocus) state.editing = true },
                onValueChange = state::onInput,
                onSearch = submit,
                onVoiceResult = submit,
                active = true,
                autoFocus = !hasResults,
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
        ) {
            when {
                showResults -> Column(Modifier.fillMaxSize()) {
                    SearchCategoryChips(
                        selected = state.category,
                        onSelect = { state.category = it },
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                    SearchResultsList(
                        state = state,
                        follow = follow,
                        openers = openers,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + navBottom),
                    )
                }

                keyword.isNotBlank() -> SuggestionList(
                    state = state,
                    onPick = submit,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = navBottom),
                )

                else -> SearchIdleContent(
                    state = state,
                    onPick = submit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
