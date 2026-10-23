package io.example.fit.feature.abcdefgh.api

import io.example.fit.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object AbcdefghListKey : AppNavKey

@Serializable
data class AbcdefghDetailKey(val id: String) : AppNavKey
