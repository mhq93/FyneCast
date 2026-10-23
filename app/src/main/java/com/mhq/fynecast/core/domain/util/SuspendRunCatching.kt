package com.mhq.fynecast.core.domain.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Same as runCatching, but rethrows cancellation. Plain runCatching would turn a cancelled
 * coroutine into a failed Result, so the screen would show an error for something the
 * user simply left.
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }