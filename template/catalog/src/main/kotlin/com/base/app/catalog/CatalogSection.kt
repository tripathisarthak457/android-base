package com.base.app.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The catalog's pages.
 *
 * An enum rather than a list of objects holding composable lambdas, because the selection is
 * persisted with `rememberSaveable` — an enum saves as its name, a lambda does not save at all,
 * and rotating the device would otherwise throw the reader back to the index.
 */
enum class CatalogSection(val title: String, val summary: String) {
    Foundations("Foundations", "Colour, type, spacing, elevation, motion"),
    Buttons("Buttons", "Five variants, three sizes, loading and disabled"),
    Inputs("Inputs", "Text, password, search, numeric, multi-line"),
    Fields("Specialised fields", "OTP, stepper, select, phone, currency, tags"),
    Selection("Selection", "Checkbox, radio, switch, slider, chips, segments"),
    Containers("Containers", "Cards, list rows, dividers, bars"),
    Lists("Lists & gestures", "Swipe, accordion, pull-to-refresh, pager, timeline"),
    Data("Data display", "Detail rows, stats, ratings, sparkline, bar chart"),
    Feedback("Feedback", "Progress, skeletons, badges, banners, empty and error"),
    Overlays("Overlays", "Dialogs, sheets, menus, tooltips, snackbars"),
    Motion("Animation", "The transition set, running, including list animation"),
    Feel("Motion & haptics", "Press feedback, motion styles, the haptic vocabulary"),
    DateTime("Date & time", "Calendar, wheels, pickers"),
    Icons("Icons", "The whole set, at a glance"),
}

@Composable
fun CatalogSection.Content() {
    when (this) {
        CatalogSection.Foundations -> FoundationsSection()
        CatalogSection.Buttons -> ButtonsSection()
        CatalogSection.Inputs -> InputsSection()
        CatalogSection.Fields -> FieldsSection()
        CatalogSection.Selection -> SelectionSection()
        CatalogSection.Containers -> ContainersSection()
        CatalogSection.Lists -> ListsSection()
        CatalogSection.Data -> DataSection()
        CatalogSection.Feedback -> FeedbackSection()
        CatalogSection.Overlays -> OverlaysSection()
        CatalogSection.Motion -> MotionSection()
        CatalogSection.Feel -> FeelSection()
        CatalogSection.DateTime -> DateTimeSection()
        CatalogSection.Icons -> IconsSection()
    }
}

/**
 * A labelled group.
 *
 * Every example on every page sits in one of these, so the pages stay readable as they grow and
 * nobody has to invent a heading style per section.
 */
@Composable
fun CatalogGroup(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(
                text = title,
                style = AppTheme.typography.headingSmall,
                color = AppTheme.colors.contentPrimary,
            )
            caption?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.contentTertiary,
                )
            }
        }
        AppDivider()
        Column(
            modifier = Modifier.padding(top = AppTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            content()
        }
    }
}
