package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.base.app.core.designsystem.foundation.draw.LocalRedactedState

/**
 * Draws [content] as placeholders while [active], as SwiftUI's `redacted(reason: .placeholder)`
 * does: its real layout, with text, icons, avatars, images, chips and filled buttons drawn as bones
 * that the window's shimmer sweeps across. Give it the real composable filled with placeholder data,
 * so the loading row is exactly as tall as the row that replaces it, at every font scale, and
 * nothing moves when the data arrives. Cards and other containers keep their own look.
 *
 * While active the content takes no input and is hidden from screen readers, which hear
 * [contentDescription] instead: pass it once for a loading region, not for every row. Toggling
 * [active] keeps the content's composition and state.
 */
@Composable
fun AppRedacted(
    active: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val hidden = if (active) {
        Modifier.clearAndSetSemantics { if (contentDescription != null) this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    CompositionLocalProvider(LocalRedactedState provides active) {
        Box(modifier = modifier.then(hidden), propagateMinConstraints = true) { content() }
    }
}
