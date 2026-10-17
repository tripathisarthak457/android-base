package com.example.modulescompact.data.wallet.api

import com.example.modulescompact.core.common.result.AppResult

/**
 * Everything the app can ask about wallet.
 * An interface, so ViewModel tests use a fake.
 */
interface WalletRepository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<WalletItem>>

    suspend fun item(id: String): AppResult<WalletItem>
}
