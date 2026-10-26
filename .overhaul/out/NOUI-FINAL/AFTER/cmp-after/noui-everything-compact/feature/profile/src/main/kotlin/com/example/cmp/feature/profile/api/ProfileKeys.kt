// Every feature's keys are in its api package's <Feature>Keys.kt, one key or several.
@file:Suppress("ktlint:standard:filename")

package com.example.cmp.feature.profile.api

import com.example.cmp.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object ProfileKey : AppNavKey
