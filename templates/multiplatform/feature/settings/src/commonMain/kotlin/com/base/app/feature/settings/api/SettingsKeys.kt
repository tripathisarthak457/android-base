package com.base.app.feature.settings.api

import com.base.app.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/** Settings is a destination like any other, whether it is a tab root or pushed from a row. */
@Serializable
data object SettingsKey : AppNavKey

/** What the settings screen needs to know about the build it is running in. Supplied by the app. */
data class SettingsAppInfo(val versionName: String)
