package com.base.app.shared

/**
 * What differs between builds of the app, handed in by each platform at start: from `BuildConfig`
 * on Android, whose flavours set it, and from the xcconfig of the iOS configuration.
 */
data class AppEnvironment(
    val apiBaseUrl: String,
    /** `dev` or `prod`. */
    val name: String,
    val isDebug: Boolean,
    val versionName: String,
)
