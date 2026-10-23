package com.example.videoalone.feature.home.api

import com.example.videoalone.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object HomeListKey : AppNavKey

@Serializable
data class HomeDetailKey(val id: String) : AppNavKey
