package com.base.app.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How deep in presentations this content is: 0 in the shell, d in the d-th entry presented over
 * it. A sheet opened from here takes the next level, so it recedes what it covers and no more.
 */
val LocalPresentationLevel: ProvidableCompositionLocal<Int> = staticCompositionLocalOf { 0 }
