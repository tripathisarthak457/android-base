package com.example.finalafter.crui2.feature.auth

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers. */
internal fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
}
