package com.example.wp04.base.feature.wallet

import com.example.wp04.base.core.navigation.AppNavKey
import kotlinx.serialization.Serializable

/** This feature's destinations. Keys carry ids, never models: they are saved in the bundle. */
@Serializable
data object WalletListKey : AppNavKey

@Serializable
data class WalletDetailKey(val itemId: Int) : AppNavKey
