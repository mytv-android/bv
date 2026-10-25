package dev.aaa1115910.bv.ui.feature.settings

import dev.aaa1115910.bv.ui.designsystem.component.FullPageLoading
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.aaa1115910.bv.ui.PreferenceWrites
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Load the initial values before presenting editable controls, without blocking composition. */
@Composable
internal fun <T : Any> rememberPreferenceSnapshot(load: () -> T): T? {
    var snapshot by remember { mutableStateOf<T?>(null) }
    LaunchedEffect(Unit) {
        PreferenceWrites.awaitPending()
        snapshot = withContext(Dispatchers.IO) { load() }
    }
    if (snapshot == null) FullPageLoading()
    return snapshot
}
