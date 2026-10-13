package __PKG__.data.__NAME__.api

import __PKG__.core.common.result.AppResult

/**
 * Answers with [result] and counts the list loads, so a test can check that a screen does not load
 * twice. Set [result] between events to script a failure or a recovery.
 */
class Fake__P__Repository(
    var result: AppResult<List<__P__Item>>,
) : __P__Repository {

    var loads = 0
        private set

    override suspend fun items(forceRefresh: Boolean): AppResult<List<__P__Item>> {
        loads++
        return result
    }

    override suspend fun item(id: String): AppResult<__P__Item> =
        when (val current = result) {
            is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
            is AppResult.Failure -> current
        }
}
