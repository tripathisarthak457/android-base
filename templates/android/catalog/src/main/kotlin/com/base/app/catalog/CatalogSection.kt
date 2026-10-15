package com.base.app.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.base.app.catalog.sections.AdaptiveSection
import com.base.app.catalog.sections.BarsSection
import com.base.app.catalog.sections.ButtonsSection
// <opt:collections>
import com.base.app.catalog.sections.CollectionsSection
// </opt:collections>
import com.base.app.catalog.sections.ContainersSection
// <opt:datadisplay>
import com.base.app.catalog.sections.DataSection
// </opt:datadisplay>
import com.base.app.catalog.sections.FeedbackSection
import com.base.app.catalog.sections.FeelSection
// <opt:formfields>
import com.base.app.catalog.sections.FieldsSection
// </opt:formfields>
import com.base.app.catalog.sections.FoundationsSection
import com.base.app.catalog.sections.InputsSection
import com.base.app.catalog.sections.ListsSection
import com.base.app.catalog.sections.MotionSection
import com.base.app.catalog.sections.OverlaysSection
// <opt:pickers>
import com.base.app.catalog.sections.PickersSection
// </opt:pickers>
import com.base.app.catalog.sections.SelectionSection
import com.base.app.catalog.sections.TextSection
import com.base.app.core.designsystem.component.list.AppSectionHeader
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.catalog.sections.NavigationSection
import com.base.app.core.designsystem.theme.AppTheme

/** The catalog's pages. The name is what `--es section` takes. */
enum class CatalogSection(val title: String, val summary: String) {
    Foundations("Foundations", "Colour roles, type scale, spacing, shapes, sizes, springs"),
    Feel("Press and motion", "Press feedback side by side, motion styles, the haptic vocabulary"),
    Text("Text and icons", "Text, figures, expandable text, the icon set"),
    Buttons("Buttons", "Filled, tinted, plain, destructive; sizes, icons, loading, links"),
    Inputs("Inputs", "Text fields, secure field, search with scopes"),
    Selection("Selection", "Switch, checkbox, radio, segmented control, chips, slider"),
    Lists("Lists", "Grouped sections, rows, toggle rows, disclosure, swipe actions, refresh"),
    Containers("Containers", "Cards, surfaces, dividers, scaffold, bottom action bar"),
    Bars("Bars and pages", "Inline and large-title top bars, tab row, pager and page control"),
    Overlays("Overlays", "Alert, action sheet, sheet with detents, menu, toast, loading"),
    Feedback("Feedback", "States, banners, skeletons, spinners, progress, badges, avatars"),
    Loading("Loading screen", "Redacted rows under a large title: one sweep, and the swap to content"),
    Motion("Animation", "Transitions, first appearance, list placement, emphasis, rubber band"),
    Adaptive("Adaptive", "Panes of any width, foldable postures, the hardware keyboard"),
    Navigation("Navigation", "The tab bar in every variant, push, pop, cover and back"),
    // <opt:pickers>
    Pickers("Pickers", "Calendar, wheels, time, compact date and time pills"),
    // </opt:pickers>
    // <opt:formfields>
    Fields("Form fields", "Code, decimal, money, phone, stepper, tags, select, range, steps"),
    // </opt:formfields>
    // <opt:datadisplay>
    Data("Data display", "Stats, charts, key values, timeline, table, counter, rating"),
    // </opt:datadisplay>
    // <opt:collections>
    Collections("Collections", "Carousel, reorderable list, image viewer"),
    // </opt:collections>
}

@Composable
fun CatalogSection.Content() {
    when (this) {
        CatalogSection.Foundations -> FoundationsSection()
        CatalogSection.Feel -> FeelSection()
        CatalogSection.Text -> TextSection()
        CatalogSection.Buttons -> ButtonsSection()
        CatalogSection.Inputs -> InputsSection()
        CatalogSection.Selection -> SelectionSection()
        CatalogSection.Lists -> ListsSection()
        CatalogSection.Containers -> ContainersSection()
        CatalogSection.Bars -> BarsSection()
        CatalogSection.Overlays -> OverlaysSection()
        CatalogSection.Feedback -> FeedbackSection()
        CatalogSection.Loading -> error("Loading is a whole screen, not a page of groups")
        CatalogSection.Navigation -> NavigationSection()
        CatalogSection.Motion -> MotionSection()
        CatalogSection.Adaptive -> AdaptiveSection()
        // <opt:pickers>
        CatalogSection.Pickers -> PickersSection()
        // </opt:pickers>
        // <opt:formfields>
        CatalogSection.Fields -> FieldsSection()
        // </opt:formfields>
        // <opt:datadisplay>
        CatalogSection.Data -> DataSection()
        // </opt:datadisplay>
        // <opt:collections>
        CatalogSection.Collections -> CollectionsSection()
        // </opt:collections>
    }
}

/**
 * A labelled group. Every example on every page sits in one, so pages stay readable as they grow
 * and nobody invents a heading style per section.
 */
@Composable
fun CatalogGroup(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        Column {
            AppSectionHeader(title = title)
            caption?.let { AppText(text = it, style = AppTheme.typography.footnote, color = AppTheme.colors.contentSecondary) }
        }
        content()
    }
}
