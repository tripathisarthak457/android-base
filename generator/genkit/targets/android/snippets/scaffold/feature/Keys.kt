package __PKG__.feature.__NAME__.api

import __PKG__.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object __P__ListKey : AppNavKey

@Serializable
data class __P__DetailKey(val id: String) : AppNavKey
