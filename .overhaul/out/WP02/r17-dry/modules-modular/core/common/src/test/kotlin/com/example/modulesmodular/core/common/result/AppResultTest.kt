package com.example.modulesmodular.core.common.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppResultTest {

    @Test
    fun `map transforms a success and passes a failure through untouched`() {
        assertEquals(AppResult.Success(4), AppResult.Success(2).map { it * 2 })

        val failure: AppResult<Int> = AppResult.Failure(AppError.Offline)
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `onSuccess runs only for a success`() {
        val seen = mutableListOf<Int>()

        AppResult.Success(1).onSuccess { seen += it }
        AppResult.Failure(AppError.Timeout).onSuccess { seen += 2 }

        assertEquals(listOf(1), seen)
    }

    @Test
    fun `errorOrNull is the failure's error, and null for a success`() {
        assertEquals(AppError.Http(status = 404), AppResult.Failure(AppError.Http(status = 404)).errorOrNull())
        assertNull(AppResult.Success(Unit).errorOrNull())
    }
}
