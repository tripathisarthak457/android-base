package com.base.app.feature.settings.language

import androidx.compose.runtime.Composable

/**
 * Opens the system's language page for this app, or null where the system has none (Android
 * before 13). Both platforms keep a per-app language there, and the strings follow it on the next
 * frame, so the app keeps no language setting of its own.
 */
@Composable
internal expect fun rememberLanguageSettings(): (() -> Unit)?
