package com.example.wp04.abstd.core.common.result

/** The outcome of anything that talks to the outside world. */
sealed interface AppResult<out T> {

    data class Success<out T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Success) action(value)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value

fun AppResult<*>.errorOrNull(): AppError? = (this as? AppResult.Failure)?.error
