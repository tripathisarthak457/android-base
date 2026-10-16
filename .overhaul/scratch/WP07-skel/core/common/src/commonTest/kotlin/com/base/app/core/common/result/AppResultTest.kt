package com.base.app.core.common.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppResultTest {

    @Test
    fun map_transformsSuccess_passesFailureThrough() {
        assertEquals(AppResult.Success(4), AppResult.Success(2).map { it * 2 })

        val failure: AppResult<Int> = AppResult.Failure(AppError.Offline)
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun onSuccess_runsOnlyForSuccess() {
        val seen = mutableListOf<Int>()

        AppResult.Success(1).onSuccess { seen += it }
        AppResult.Failure(AppError.Timeout).onSuccess { seen += 2 }

        assertEquals(listOf(1), seen)
    }

    @Test
    fun errorOrNull_isTheFailuresError_nullForSuccess() {
        assertEquals(
            AppError.Http(status = 404),
            AppResult.Failure(AppError.Http(status = 404)).errorOrNull(),
        )
        assertNull(AppResult.Success(Unit).errorOrNull())
    }
}
