package com.base.app.core.ui

import androidx.compose.runtime.Composable
import com.base.app.core.mvi.UiText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Resolves a [UiText] in the language the app is showing. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *args.toTypedArray())
}
