package com.example.finalafter.crnoui.core.ui

import com.example.finalafter.crnoui.core.common.result.AppError
import com.example.finalafter.crnoui.core.mvi.UiText
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorTextTest {

    private val fallback = UiText.Dynamic("the screen's own sentence")

    @Test
    fun `no connection and no answer each get their own sentence`() {
        assertEquals(UiText.of(R.string.ui_error_offline), AppError.Offline.toUiText(fallback))
        assertEquals(UiText.of(R.string.ui_error_unreachable), AppError.Timeout.toUiText(fallback))
    }

    @Test
    fun `the server's own words are shown as they came`() {
        val error = AppError.Http(status = 409, message = "That name is taken")

        assertEquals(UiText.Dynamic("That name is taken"), error.toUiText(fallback))
    }

    @Test
    fun `a server error without words of its own falls back`() {
        val silent = listOf(
            AppError.Http(status = 500),
            AppError.Http(status = 500, message = "  "),
        )

        silent.forEach { error -> assertEquals("$error", fallback, error.toUiText(fallback)) }
    }

    @Test
    fun `failures the person cannot act on fall back`() {
        val cause = IOException("disk full")
        val failures = listOf(
            AppError.Decode(cause),
            AppError.Storage(cause),
            AppError.Unexpected(cause),
        )

        failures.forEach { error -> assertEquals("$error", fallback, error.toUiText(fallback)) }
    }

    @Test
    fun `without a fallback of the screen's own the generic sentence is used`() {
        assertEquals(UiText.of(R.string.ui_error_generic), AppError.Http(status = 500).toUiText())
    }
}
