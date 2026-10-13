package __PKG__.data.__NAME__.api

import __PKG__.core.common.result.AppResult

/**
 * Answers with [result] and counts the list loads, so a test can check that a screen does not load
 * twice. Set [result] between events to script a failure or a recovery.
 */
__FIT__
class Fake__P__Repository(var result: AppResult<List<__P__Item>>) : __P__Repository {
__ELSE__
class Fake__P__Repository(var result: AppResult<List<__P__Item>>) :
    __P__Repository {
__ELSE__
class Fake__P__Repository(
    var result: AppResult<List<__P__Item>>,
) : __P__Repository {
__END_FIT__

    var loads = 0
        private set

__FIT__
    override suspend fun items(forceRefresh: Boolean): AppResult<List<__P__Item>> {
__ELSE__
    override suspend fun items(
        forceRefresh: Boolean,
    ): AppResult<List<__P__Item>> {
__END_FIT__
        loads++
        return result
    }

__FIT__
    override suspend fun item(id: String): AppResult<__P__Item> = when (val current = result) {
        is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
        is AppResult.Failure -> current
    }
__ELSE__
    override suspend fun item(id: String): AppResult<__P__Item> =
        when (val current = result) {
            is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
            is AppResult.Failure -> current
        }
__END_FIT__
}
