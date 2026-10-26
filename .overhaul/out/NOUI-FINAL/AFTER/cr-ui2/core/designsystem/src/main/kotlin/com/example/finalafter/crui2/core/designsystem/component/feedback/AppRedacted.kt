package com.example.finalafter.crui2.core.designsystem.component.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.foundation.draw.LocalRedactedState
import com.example.finalafter.crui2.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme

/**
 * Draws [content] as placeholders while [active], as SwiftUI's `redacted(reason: .placeholder)`
 * does: its real layout, drawn as bones that the window's shimmer sweeps across. Give it the real
 * composable filled with placeholder data, so the loading row is exactly as tall as the row that
 * replaces it, at every font scale, and nothing moves when the data arrives.
 *
 * Text, icons, avatars, network images, chips, badges, status pills and buttons with a fill draw as
 * bones in their own shape; cards and other containers keep their own look. Controls built on
 * `Modifier.pressable`, such as buttons, cards, chips, `AppRow`, `AppChoiceRow` and
 * `AppSelectField`, take no tap, key or focus. Controls built any other way are not redacted:
 * `AppSwitch`, `AppCheckbox`, `AppRadio`, `AppToggleRow`, `AppSegmentedControl`, `AppSlider`,
 * `AppRangeSlider`, `AppStepper` and the text fields among them draw and respond as usual, so keep
 * them out of placeholder content. A custom control reads `LocalRedacted` to draw
 * `Modifier.skeleton` and take no input.
 *
 * The content is hidden from screen readers, which hear [contentDescription] instead: pass it once
 * for a loading region, not for every row. Toggling [active] keeps the content's composition and
 * state.
 *
 * The placeholder data is never drawn or read out, so it is not a string resource. Only where its
 * lines break shapes the bones: give it the length typical content has at phone width.
 *
 * A loading list is a list of its own: the real rows, redacted, with the loaded list's content
 * padding and a `LazyListState` of its own, replaced by the loaded list in one frame. A list state's
 * item animations follow the rows it laid out last, so on a shared one, whenever both lists are on
 * screen, as in a cross-fade, the loaded rows are compared with the placeholders and every one fades
 * in as new.
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

@AppComponentPreviews
@Composable
private fun AppRedactedPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppRedacted(active = true, contentDescription = "Loading the balance") {
            AppText(text = "Balance 1,204.50", style = AppTheme.typography.title2)
        }
        AppRedacted(active = false) {
            AppText(text = "Balance 1,204.50", style = AppTheme.typography.title2)
        }
    }
}
