package com.example.everything.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The shell's layers: content, and the chrome drawn over it. A plain box; with liquid glass on,
 * this is where the glass stage records the content for the chrome to sample.
 */
@Composable
internal fun ShellChrome(
    modifier: Modifier,
    chrome: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        content()
        chrome()
    }
}
