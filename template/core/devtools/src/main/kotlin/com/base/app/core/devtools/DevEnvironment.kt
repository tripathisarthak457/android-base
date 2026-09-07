package com.base.app.core.devtools

/**
 * What the badge says, and whether it appears at all.
 *
 * Supplied by the application module from its own `BuildConfig`, for the same reason
 * `NetworkConfig` is: a library module that reads its own `BuildConfig` reports whichever variant
 * compiled *it*, not the app.
 *
 * [visible] is the whole safety rule in one place. A production build shows nothing and records
 * nothing — see the module's own documentation for why absence beats a runtime check.
 */
data class DevEnvironment(
    val name: String,
    val versionName: String,
    val applicationId: String,
    val apiBaseUrl: String,
    val isDebugBuild: Boolean,
    val isShippable: Boolean,
) {
    /** Debug builds always; dev and staging releases too; production never. */
    val visible: Boolean get() = isDebugBuild || !isShippable

    /** `DEV · debug`, which is what somebody holding two installs needs to tell them apart. */
    val label: String get() = buildString {
        append(name.uppercase())
        if (isDebugBuild) append(" · debug")
    }
}
