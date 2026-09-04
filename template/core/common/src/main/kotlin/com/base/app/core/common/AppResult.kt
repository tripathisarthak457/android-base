package com.base.app.core.common

/**
 * The outcome of anything that talks to the outside world.
 *
 * Named `AppResult` rather than `Result` so it never shadows `kotlin.Result`; the two appear in
 * the same file often enough — `runCatching` returns one and repositories return the other — that
 * a shadowed import is a genuine source of confusion.
 *
 * The [Failure] fields exist because a screen has to make different decisions for each of them,
 * and inferring those from a `Throwable` at the UI layer means the UI layer importing the network
 * layer's exception types. [isOffline] in particular separates "you are not connected" from
 * "the server said no", which every design treats as two different states with two different
 * calls to action.
 */
sealed interface AppResult<out T> {

    data class Success<T>(
        val data: T,
        /** True when this came from the local cache rather than the network. */
        val fromCache: Boolean = false,
        /** When [fromCache], the epoch-millis the entry was stored, for a "last synced" line. */
        val cachedAtEpochMillis: Long? = null,
    ) : AppResult<T>

    data class Failure(
        val message: String? = null,
        val cause: Throwable? = null,
        /** The HTTP status, when there was one. Null for transport failures. */
        val code: Int? = null,
        /** Per-field messages from a validation response, keyed by field name. */
        val fieldErrors: Map<String, List<String>> = emptyMap(),
        /** True when the request never reached the server — no connectivity, or a timeout. */
        val isOffline: Boolean = false,
        /** The raw response body, kept for endpoints whose error shape does not match the rest. */
        val rawBody: String? = null,
    ) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data), fromCache, cachedAtEpochMillis)
    is AppResult.Failure -> this
}

/** [map] for a transform that can itself fail — a decode, or a business rule rejecting the data. */
inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(data)
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Success) action(data)
}

inline fun <T> AppResult<T>.onFailure(action: (AppResult.Failure) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Failure) action(this)
}

/** Collapses both branches to a single value — the shape a `when` in a reducer usually wants. */
inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppResult.Failure) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(this)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

fun <T> AppResult<T>.getOrDefault(fallback: T): T = getOrNull() ?: fallback

/**
 * Runs [block], converting anything it throws into [AppResult.Failure].
 *
 * `CancellationException` is deliberately re-thrown. Swallowing it turns a cancelled coroutine
 * into a spurious error toast and, worse, stops the cancellation propagating — the caller's scope
 * believes the child completed normally and carries on.
 */
inline fun <T> resultOf(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: kotlinx.coroutines.CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    AppResult.Failure(message = throwable.message, cause = throwable)
}
