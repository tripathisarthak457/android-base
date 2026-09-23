package com.base.app.core.common.util

import android.util.Log

/** Logging, behind one seam. */
object AppLogger {

    private const val DEFAULT_TAG = "BaseApp"

    @Volatile
    var debugEnabled: Boolean = false

    /**
     * Set by the application module when a crash reporter is present, so that every logged error
     * also becomes a breadcrumb on the next crash. Left null, logging stays local.
     */
    @Volatile
    var reporter: ((message: String, throwable: Throwable?) -> Unit)? = null

    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (debugEnabled) Log.d(tag, message)
    }

    fun i(message: String, tag: String = DEFAULT_TAG) {
        if (debugEnabled) Log.i(tag, message)
    }

    fun w(message: String, tag: String = DEFAULT_TAG, throwable: Throwable? = null) {
        if (debugEnabled) Log.w(tag, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        Log.e(tag, message, throwable)
        reporter?.invoke("$tag: $message", throwable)
    }
}
