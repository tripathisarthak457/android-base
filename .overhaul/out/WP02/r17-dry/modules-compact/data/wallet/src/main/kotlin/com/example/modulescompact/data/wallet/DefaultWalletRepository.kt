package com.example.modulescompact.data.wallet

import com.example.modulescompact.core.common.result.AppResult
import com.example.modulescompact.core.common.result.map
import com.example.modulescompact.core.network.NetworkClient
import com.example.modulescompact.core.network.get
import com.example.modulescompact.core.network.model.CachePolicy
import com.example.modulescompact.data.wallet.api.WalletItem
import com.example.modulescompact.data.wallet.api.WalletRepository
import com.example.modulescompact.data.wallet.remote.WalletDto
import com.example.modulescompact.data.wallet.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultWalletRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : WalletRepository {

    /** Cached, so reopening the screen or going offline shows the last answer. */
    override suspend fun items(forceRefresh: Boolean): AppResult<List<WalletItem>> =
        networkClient.get<List<WalletDto>>(
            path = PATH,
            cache = CachePolicy.Enabled(
                key = PATH,
                maxAgeMillis = CACHE_MAX_AGE_MILLIS,
                forceRefresh = forceRefresh,
            ),
        ).map { dtos -> dtos.map(WalletDto::toDomain) }

    override suspend fun item(id: String): AppResult<WalletItem> = networkClient.get<WalletDto>(
        path = "$PATH/$id",
        cache = CachePolicy.Enabled(key = "$PATH/$id", maxAgeMillis = CACHE_MAX_AGE_MILLIS),
    ).map(WalletDto::toDomain)

    private companion object {
        const val PATH = "wallet"
        const val CACHE_MAX_AGE_MILLIS = 5 * 60 * 1000L
    }
}
