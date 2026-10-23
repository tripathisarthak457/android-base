package io.example.fitcnet.feature.abcdefghi.api

import io.example.fitcnet.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object AbcdefghiListKey : AppNavKey

@Serializable
data class AbcdefghiDetailKey(val id: String) : AppNavKey
