package io.example.fitcnet.core.designsystem.foundation.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/** The single definition of what "disabled" looks like. */
fun Modifier.disabledAlpha(enabled: Boolean): Modifier = if (enabled) this else alpha(DisabledAlpha)

const val DisabledAlpha = 0.38f
