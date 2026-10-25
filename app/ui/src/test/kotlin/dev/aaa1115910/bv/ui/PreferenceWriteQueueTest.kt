package dev.aaa1115910.bv.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class PreferenceWriteQueueTest {
    @Test
    fun `writes remain pending until their FIFO actions complete`() = withQueue { fixture ->
        val events = mutableListOf<Int>()
        val first = fixture.queue.enqueue { events += 1 }
        val second = fixture.queue.enqueue { events += 2 }

        assertTrue(fixture.queue.hasPending)
        assertFalse(first.isCompleted)
        assertFalse(second.isCompleted)
        fixture.dispatcher.runUntilIdle()

        assertEquals(listOf(1, 2), events)
        assertTrue(first.isCompleted)
        assertTrue(second.isCompleted)
        assertFalse(fixture.queue.hasPending)
    }

    @Test
    fun `pending slider saves retain only the latest value per key`() = withQueue { fixture ->
        val events = mutableListOf<String>()
        val first = fixture.queue.enqueueLatest("scale") { events += "old-scale" }
        val last = fixture.queue.enqueueLatest("scale") { events += "new-scale" }
        fixture.queue.enqueueLatest("opacity") { events += "opacity" }

        assertSame(first, last)
        fixture.dispatcher.runUntilIdle()
        assertEquals(listOf("new-scale", "opacity"), events)
        assertFalse(fixture.queue.hasPending)
    }

    @Test
    fun `ordinary actions separate coalescing epochs`() = withQueue { fixture ->
        val events = mutableListOf<String>()
        fixture.queue.enqueueLatest("scale") { events += "before" }
        fixture.queue.enqueue { events += "read" }
        val after = fixture.queue.enqueueLatest("scale") { events += "after" }
        val latest = fixture.queue.enqueueLatest("scale") { events += "latest" }

        assertSame(after, latest)
        fixture.dispatcher.runUntilIdle()
        assertEquals(listOf("before", "read", "latest"), events)
    }

    @Test
    fun `read barrier cannot replace a preceding save with a later value`() = withQueue { fixture ->
        val events = mutableListOf<Int>()
        fixture.queue.enqueueLatest("scale") { events += 1 }
        var barrierFinished = false
        fixture.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            fixture.queue.awaitPending()
            barrierFinished = true
        }
        fixture.queue.enqueueLatest("scale") { events += 2 }
        fixture.queue.enqueueLatest("scale") { events += 3 }

        assertFalse(barrierFinished)
        fixture.dispatcher.runUntilIdle()
        assertEquals(listOf(1, 3), events)
        assertTrue(barrierFinished)
        assertFalse(fixture.queue.hasPending)
    }

    @Test
    fun `a save submitted during an active write is not lost`() = withQueue { fixture ->
        val events = mutableListOf<Int>()
        var later: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        val first = fixture.queue.enqueueLatest("scale") {
            events += 1
            later = fixture.queue.enqueueLatest("scale") { events += 2 }
        }

        fixture.dispatcher.runUntilIdle()
        assertNotSame(first, later)
        assertEquals(listOf(1, 2), events)
        assertTrue(later!!.isCompleted)
        assertFalse(fixture.queue.hasPending)
    }

    @Test
    fun `failed saves report failure without stopping subsequent writes`() = withQueue { fixture ->
        val failure = IllegalStateException("save failed")
        val failed = fixture.queue.enqueue { throw failure }
        var nextSaved = false
        fixture.queue.enqueue { nextSaved = true }

        fixture.dispatcher.runUntilIdle()
        assertSame(failure, assertFailsWith<IllegalStateException> { runBlocking { failed.await() } })
        assertEquals(listOf(failure), fixture.failures)
        assertTrue(nextSaved)
        assertFalse(fixture.queue.hasPending)
    }

    @Test
    fun `cancelling a screen waiting for a save does not cancel the save`() = withQueue { fixture ->
        var saved = false
        val accepted = fixture.queue.enqueue { saved = true }
        val screen = fixture.scope.launch(start = CoroutineStart.UNDISPATCHED) { accepted.await() }
        screen.cancel()

        fixture.dispatcher.runUntilIdle()
        assertTrue(saved)
        assertTrue(accepted.isCompleted)
        assertFalse(accepted.isCancelled)
        assertFalse(fixture.queue.hasPending)
    }

    private fun withQueue(test: (QueueFixture) -> Unit) {
        val fixture = QueueFixture()
        try {
            test(fixture)
        } finally {
            fixture.scope.cancel()
            fixture.dispatcher.runUntilIdle()
        }
    }

    private class QueueFixture {
        val dispatcher = ManualDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val failures = mutableListOf<Exception>()
        val queue = PreferenceWriteQueue(scope) { failures += it }
    }

    /** No delays, real threads, Android calls, or coroutine-test dependency. */
    private class ManualDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            tasks.addLast(block)
        }

        fun runUntilIdle() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }
}
