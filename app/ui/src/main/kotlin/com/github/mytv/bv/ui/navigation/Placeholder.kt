package com.github.mytv.bv.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.mytv.bv.appui.R
import com.github.mytv.bv.ui.designsystem.theme.BvType

/** 尚未实现的页面占位。 */
@Composable
fun PlaceholderPage(title: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$title · ${stringResource(R.string.shell_developing)}",
            style = BvType.PageTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
