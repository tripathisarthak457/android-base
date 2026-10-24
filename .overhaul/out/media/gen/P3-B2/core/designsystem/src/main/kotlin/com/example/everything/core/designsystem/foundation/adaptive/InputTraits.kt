package com.example.everything.core.designsystem.foundation.adaptive

import androidx.compose.runtime.Immutable

/**
 * What the person can type and point with right now. A mouse or trackpad ([finePointer]) wants
 * hover states and smaller targets are reachable; a hardware keyboard wants focus that can be seen
 * and shortcuts; neither replaces touch, which is assumed until the window says otherwise.
 */
@Immutable
data class InputTraits(
    val finePointer: Boolean = false,
    val hardwareKeyboard: Boolean = false,
    val touchscreen: Boolean = true,
)
