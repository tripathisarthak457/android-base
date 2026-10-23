package io.example.fit.feature.ab.api

import io.example.fit.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object AbListKey : AppNavKey

@Serializable
data class AbDetailKey(val id: String) : AppNavKey
