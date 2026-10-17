package dev.aaa1115910.bv.util

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException

private const val MAX_CAUSE_CHAIN_DEPTH = 32

/**
 * 崩溃日志记录策略：判断未捕获异常是否值得写入崩溃日志文件。
 *
 * 可恢复的网络错误和协程取消通常不是应用缺陷，记录它们只会淹没真正有价值的
 * 崩溃现场；链路遍历带深度上限，避免异常的 cause 链成环时死循环。
 */
object ExceptionPolicy {
    /** 是否值得记录到崩溃日志。 */
    fun shouldRecord(throwable: Throwable): Boolean =
        !throwable.isExpectedNetworkFailure() && !throwable.isCancellationFailure()
}

/** Returns whether this failure, or one of its causes, is a recoverable connectivity error. */
fun Throwable.isExpectedNetworkFailure(): Boolean = anyCause { error ->
    error is UnknownHostException ||
        error is SocketTimeoutException ||
        error is ConnectException ||
        error is NoRouteToHostException
}

internal fun Throwable.isCancellationFailure(): Boolean = anyCause { error ->
    error is CancellationException
}

private inline fun Throwable.anyCause(predicate: (Throwable) -> Boolean): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_CHAIN_DEPTH) {
        val error = current ?: return false
        if (predicate(error)) return true
        current = error.cause?.takeUnless { it === error }
    }
    return false
}
