package com.example.wp04.dlplay.core.common.result

/**
 * Why something that talks to the outside world failed. Closed, so a `when` over it is checked by
 * the compiler and every screen decides what each case means for the person using it.
 */
sealed interface AppError {

    /** The device has no connection. Retrying makes sense once it has one. */
    data object Offline : AppError

    /** The device is online but no answer came back in time. */
    data object Timeout : AppError

    /**
     * The server answered with an error status. [code] is the backend's own error code and
     * [fieldErrors] its per-field messages, keyed by the field names the request used.
     */
    data class Http(
        val status: Int,
        val code: String? = null,
        val message: String? = null,
        val fieldErrors: Map<String, List<String>> = emptyMap(),
        val retryAfterSeconds: Long? = null,
    ) : AppError

    /** The server answered, but not in the shape the app expects. */
    data class Decode(val cause: Throwable) : AppError

    /** The Keystore, DataStore or the database refused a read or a write. */
    data class Storage(val cause: Throwable) : AppError

    /** A bug: something threw that nothing above it was written to expect. */
    data class Unexpected(val cause: Throwable) : AppError
}
