package com.base.app.core.ui

import androidx.compose.runtime.Composable
import com.base.app.core.common.result.AppError
import com.base.app.core.mvi.UiText
import com.base.app.core.ui.resources.Res
import com.base.app.core.ui.resources.common_error_generic
import com.base.app.core.ui.resources.common_error_offline
import com.base.app.core.ui.resources.common_error_unreachable

/**
 * What to tell the person about a failure: the server's own words when it sent some, otherwise a
 * sentence in the app's language for what went wrong, and [fallback] for anything else. The
 * server's text is shown as it is, so the backend is expected to localise it from Accept-Language.
 */
fun AppError.toUiText(fallback: UiText = UiText.of(Res.string.common_error_generic)): UiText = when (this) {
    AppError.Offline -> UiText.of(Res.string.common_error_offline)
    AppError.Timeout -> UiText.of(Res.string.common_error_unreachable)
    is AppError.Http -> message?.takeIf { it.isNotBlank() }?.let(UiText::Dynamic) ?: fallback
    is AppError.Decode, is AppError.Storage, is AppError.Unexpected -> fallback
}

@Composable
fun AppError.text(fallback: UiText = UiText.of(Res.string.common_error_generic)): String =
    toUiText(fallback).asString()
