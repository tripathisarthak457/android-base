package com.example.modulesmodular.feature.trips.api

import com.example.modulesmodular.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object TripsListKey : AppNavKey

@Serializable
data class TripsDetailKey(val id: String) : AppNavKey
