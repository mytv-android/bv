package com.github.mytv.bv.util

import androidx.compose.runtime.snapshots.SnapshotStateList

fun <T> SnapshotStateList<T>.swapList(newList: List<T>) {
    clear()
    addAll(newList)
}
