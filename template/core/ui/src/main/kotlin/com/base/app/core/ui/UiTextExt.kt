package com.base.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.base.app.core.common.util.UiText

/**
 * Resolves a [UiText] against the current configuration.
 *
 * Reading through `LocalContext` rather than a captured one is what makes a locale change take
 * effect immediately: the composition is recreated with a new context, and every string resolves
 * again. A ViewModel that had formatted the string itself would still be holding the old one.
 */
@Composable
@ReadOnlyComposable
fun UiText.asString(): String = resolve(LocalContext.current)

@Composable
@ReadOnlyComposable
fun UiText?.asStringOrEmpty(): String = this?.resolve(LocalContext.current).orEmpty()
