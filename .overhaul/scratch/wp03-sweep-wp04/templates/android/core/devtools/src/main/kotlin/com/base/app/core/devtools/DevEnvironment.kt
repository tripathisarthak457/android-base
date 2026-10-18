package com.base.app.core.devtools

/** What the badge says, and whether it appears at all. */
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
