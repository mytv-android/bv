package com.github.mytv.bv.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.onBackPressed(
    onBackPressed: () -> Unit
): Modifier = then(
    Modifier.onKeyEvent {
        if (it.key == Key.Back) {
            if (it.type == KeyEventType.KeyUp) onBackPressed()
            true
        } else {
            false
        }
    }
)