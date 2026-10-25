package com.base.app.core.designsystem.foundation.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/** The single definition of what "disabled" looks like. */
public fun Modifier.disabledAlpha(enabled: Boolean): Modifier = if (enabled) this else alpha(DisabledAlpha)

public const val DisabledAlpha: Float = 0.38f
