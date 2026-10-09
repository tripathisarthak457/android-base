package com.base.app.core.designsystem.foundation.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/** The single definition of what "disabled" looks like. */
fun Modifier.disabledAlpha(enabled: Boolean): Modifier = if (enabled) this else alpha(DISABLED_ALPHA)

const val DISABLED_ALPHA = 0.38f
