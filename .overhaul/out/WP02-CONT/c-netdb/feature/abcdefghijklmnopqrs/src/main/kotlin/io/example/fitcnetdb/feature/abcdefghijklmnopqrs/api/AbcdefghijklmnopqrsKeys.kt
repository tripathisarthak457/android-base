package io.example.fitcnetdb.feature.abcdefghijklmnopqrs.api

import io.example.fitcnetdb.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object AbcdefghijklmnopqrsListKey : AppNavKey

@Serializable
data class AbcdefghijklmnopqrsDetailKey(val id: String) : AppNavKey
