package com.example.wp04.base.data.wallet

import com.example.wp04.base.core.common.AppResult
import com.example.wp04.base.core.network.NetworkClient
import com.example.wp04.base.core.network.get
import javax.inject.Inject
import javax.inject.Singleton

/** The domain model for Wallet. Add a DTO and a mapper once the wire format differs. */
data class WalletItem(
    val id: Int,
    val title: String,
)

/** Everything the app can ask about Wallet. An interface, so tests can use a fake. */
interface WalletRepository {
    suspend fun items(): AppResult<List<WalletItem>>
}

@Singleton
class DefaultWalletRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : WalletRepository {
    override suspend fun items(): AppResult<List<WalletItem>> =
        networkClient.get<List<WalletItem>>(path = ITEMS_PATH)

    private companion object {
        const val ITEMS_PATH = "wallet"
    }
}
