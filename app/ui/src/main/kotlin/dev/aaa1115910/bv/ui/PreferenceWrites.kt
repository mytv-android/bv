package dev.aaa1115910.bv.ui

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Serializes blocking preference writes away from the UI thread.
 * Accepted saves outlive individual screens. Callers must capture values before
 * enqueueing and use enqueueLatest only for independent, replaceable values.
 */
internal object PreferenceWrites {
    private val queue = PreferenceWriteQueue(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        onFailure = { Log.e("PreferenceWrites", "Failed to save preference", it) },
    )

    val hasPending: Boolean get() = queue.hasPending

    fun enqueue(action: () -> Unit): CompletableDeferred<Unit> = queue.enqueue(action)

    fun enqueueLatest(key: String, action: () -> Unit): CompletableDeferred<Unit> =
        queue.enqueueLatest(key, action)

    suspend fun awaitPending() = queue.awaitPending()
}

/** Injectable scope keeps ordering and coalescing testable without Android. */
internal class PreferenceWriteQueue(
    scope: CoroutineScope,
    private val onFailure: (Exception) -> Unit,
) {
    private class Write(
        var action: () -> Unit,
        val completion: CompletableDeferred<Unit>,
        val key: String? = null,
    )

    private val queue = Channel<Write>(Channel.UNLIMITED)
    private val pending = AtomicInteger(0)
    private val latest = mutableMapOf<String, Write>()
    val hasPending: Boolean get() = pending.get() > 0

    init {
        scope.launch {
            for (write in queue) {
                val action = synchronized(latest) {
                    if (write.key != null && latest[write.key] === write) {
                        latest.remove(write.key)
                    }
                    write.action
                }
                val failure = try {
                    action()
                    null
                } catch (error: Exception) {
                    onFailure(error)
                    error
                }
                pending.decrementAndGet()
                if (failure == null) write.completion.complete(Unit)
                else write.completion.completeExceptionally(failure)
            }
        }
    }

    fun enqueue(action: () -> Unit): CompletableDeferred<Unit> = synchronized(latest) {
        // An opaque action or read barrier must observe exactly the saves before
        // it. Never replace one of those saves with a value submitted after it.
        latest.clear()
        submit(Write(action, CompletableDeferred()))
    }

    /** A slider replaces its not-yet-started save instead of queuing every frame. */
    fun enqueueLatest(key: String, action: () -> Unit): CompletableDeferred<Unit> = synchronized(latest) {
        val waiting = latest[key]
        if (waiting != null) {
            waiting.action = action
            waiting.completion
        } else {
            val write = Write(action, CompletableDeferred(), key)
            latest[key] = write
            submit(write)
        }
    }

    /** Called while holding [latest]'s monitor, including failed-send cleanup. */
    private fun submit(write: Write): CompletableDeferred<Unit> {
        pending.incrementAndGet()
        if (!queue.trySend(write).isSuccess) {
            pending.decrementAndGet()
            if (write.key != null && latest[write.key] === write) latest.remove(write.key)
            write.completion.completeExceptionally(IllegalStateException("Preference queue is closed"))
        }
        return write.completion
    }

    /** Wait for preceding saves before loading or acting on persisted values. */
    suspend fun awaitPending() {
        if (hasPending) enqueue {}.await()
    }
}
