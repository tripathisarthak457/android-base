package com.example.modulescompact.feature.wallet.api

import com.example.modulescompact.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * This feature's destinations and its whole public surface. Keys carry ids, never models: they are
 * saved with the back stack and restored after the process dies.
 */
@Serializable
data object WalletListKey : AppNavKey

@Serializable
data class WalletDetailKey(val id: String) : AppNavKey
