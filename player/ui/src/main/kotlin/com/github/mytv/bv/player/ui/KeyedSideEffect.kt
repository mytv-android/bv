package com.github.mytv.bv.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/**
 * Run a synchronous update once after a successful composition for these keys.
 * Unlike LaunchedEffect, this does not allocate a coroutine. Focus requests and
 * work that must wait for layout should continue to use LaunchedEffect.
 */
@Composable
internal fun KeyedSideEffect(vararg keys: Any?, effect: () -> Unit) {
    val execution = remember(*keys) { SideEffectExecution() }
    SideEffect {
        if (!execution.applied) {
            execution.applied = true
            effect()
        }
    }
}

private class SideEffectExecution {
    var applied = false
}
