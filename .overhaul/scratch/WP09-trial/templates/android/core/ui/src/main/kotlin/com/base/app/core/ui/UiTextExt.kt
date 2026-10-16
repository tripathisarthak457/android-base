package com.base.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.base.app.core.common.util.UiText

/** Resolves a [UiText] against the current configuration. */
@Composable
@ReadOnlyComposable
fun UiText.asString(): String = resolve(LocalContext.current)

@Composable
@ReadOnlyComposable
fun UiText?.asStringOrEmpty(): String = this?.resolve(LocalContext.current).orEmpty()
