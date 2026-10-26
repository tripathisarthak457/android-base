package com.example.cmp.data.home

import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.data.home.api.HomeItem
import com.example.cmp.data.home.api.HomeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The project has no network or database feature, so nothing feeds this domain yet: the list is
 * empty and the screen shows its empty state. Give it a source here; nothing above changes.
 */
@Singleton
internal class DefaultHomeRepository @Inject constructor() : HomeRepository {

    private val items = emptyList<HomeItem>()

    override suspend fun items(forceRefresh: Boolean): AppResult<List<HomeItem>> =
        AppResult.Success(items)

    override suspend fun item(id: String): AppResult<HomeItem> {
        val item = items.firstOrNull { it.id == id } ?: return AppResult.Failure(
            AppError.Unexpected(NoSuchElementException("No home item $id")),
        )
        return AppResult.Success(item)
    }
}
