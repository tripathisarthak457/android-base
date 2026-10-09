package __PKG__.data.__NAME__.api

import __PKG__.core.common.result.AppResult

/** Everything the app can ask about __TITLE_LOWER__. An interface, so ViewModel tests use a fake. */
interface __P__Repository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<__P__Item>>

    suspend fun item(id: String): AppResult<__P__Item>
}
