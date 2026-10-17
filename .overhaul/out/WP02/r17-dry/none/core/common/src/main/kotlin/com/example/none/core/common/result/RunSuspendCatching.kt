package com.example.none.core.common.result

import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatching] for a block that suspends. A cancellation is rethrown rather than caught, so the
 * coroutine running this still stops when its scope is cancelled; `runCatching` would turn it into
 * a failure and carry on.
 */
suspend inline fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
