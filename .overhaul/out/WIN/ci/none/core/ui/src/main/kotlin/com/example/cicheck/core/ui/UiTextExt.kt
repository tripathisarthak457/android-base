package com.example.cicheck.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.example.cicheck.core.mvi.UiText

/** Resolves a [UiText] against the current configuration. */
@Composable
@ReadOnlyComposable
fun UiText.asString(): String = resolve(LocalContext.current)
