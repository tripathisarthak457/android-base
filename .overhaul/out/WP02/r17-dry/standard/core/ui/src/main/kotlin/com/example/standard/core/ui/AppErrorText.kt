package com.example.standard.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.standard.core.common.result.AppError
import com.example.standard.core.mvi.UiText

/**
 * What to tell the person about a failure: the server's own words when it sent some, otherwise a
 * sentence in the app's language for what went wrong, and [fallback] for anything else. The
 * server's text is shown as it is, so the backend is expected to localise it from Accept-Language.
 */
fun AppError.toUiText(fallback: UiText = UiText.of(R.string.ui_error_generic)): UiText = when (this) {
    AppError.Offline -> UiText.of(R.string.ui_error_offline)
    AppError.Timeout -> UiText.of(R.string.ui_error_unreachable)
    is AppError.Http -> message?.takeIf { it.isNotBlank() }?.let(UiText::Dynamic) ?: fallback
    is AppError.Decode, is AppError.Storage, is AppError.Unexpected -> fallback
}

@Composable
@ReadOnlyComposable
fun AppError.text(fallback: UiText = UiText.of(R.string.ui_error_generic)): String =
    toUiText(fallback).asString()
