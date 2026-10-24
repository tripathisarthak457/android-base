package io.example.audit.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The presentation depth of this content: 0 in the shell, d in the d-th entry presented over it, and
 * a sheet's own level inside that sheet. A sheet opened from here takes a level above it, so it
 * recedes and dims what it covers and nothing beneath.
 */
@ShellSeamApi
val LocalPresentationLevel: ProvidableCompositionLocal<Int> = staticCompositionLocalOf { 0 }
