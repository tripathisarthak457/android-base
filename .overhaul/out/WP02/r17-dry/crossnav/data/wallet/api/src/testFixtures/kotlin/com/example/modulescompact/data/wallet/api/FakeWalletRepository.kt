package com.example.modulescompact.data.wallet.api

import com.example.modulescompact.core.common.result.AppResult

/**
 * Answers with [result] and counts the list loads, so a test can check that a screen does not load
 * twice. Set [result] between events to script a failure or a recovery.
 */
class FakeWalletRepository(var result: AppResult<List<WalletItem>>) : WalletRepository {

    var loads = 0
        private set

    override suspend fun items(forceRefresh: Boolean): AppResult<List<WalletItem>> {
        loads++
        return result
    }

    override suspend fun item(id: String): AppResult<WalletItem> = when (val current = result) {
        is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
        is AppResult.Failure -> current
    }
}
