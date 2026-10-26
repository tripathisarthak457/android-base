package com.example.finalafter.crui.feature.settings.api

import com.example.finalafter.crui.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * Settings is a destination like any other, whether it is a tab root or pushed from a profile row.
 */
@Serializable
data object SettingsKey : AppNavKey

@Serializable
data object LicensesKey : AppNavKey

/** What the settings screen needs to know about the build it is running in. Supplied by `:app`. */
data class SettingsAppInfo(val versionName: String)
