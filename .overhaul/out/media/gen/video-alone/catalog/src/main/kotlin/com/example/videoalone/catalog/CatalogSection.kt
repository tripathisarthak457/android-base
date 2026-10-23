package com.example.videoalone.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.videoalone.catalog.sections.AdaptiveSection
import com.example.videoalone.catalog.sections.BarsSection
import com.example.videoalone.catalog.sections.ButtonsSection
import com.example.videoalone.catalog.sections.ContainersSection
import com.example.videoalone.catalog.sections.FeedbackSection
import com.example.videoalone.catalog.sections.FeelSection
import com.example.videoalone.catalog.sections.FoundationsSection
import com.example.videoalone.catalog.sections.InputsSection
import com.example.videoalone.catalog.sections.ListsSection
import com.example.videoalone.catalog.sections.MotionSection
import com.example.videoalone.catalog.sections.NavigationSection
import com.example.videoalone.catalog.sections.OverlaysSection
import com.example.videoalone.catalog.sections.SelectionSection
import com.example.videoalone.catalog.sections.TextSection
import com.example.videoalone.catalog.sections.sheets.SheetsSection
import com.example.videoalone.core.designsystem.component.list.AppSectionHeader
import com.example.videoalone.core.designsystem.component.text.AppText
import com.example.videoalone.core.designsystem.theme.AppTheme

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
    Navigation("Navigation", "The tab bar in every variant, push, pop, cover and back"),
    Overlays("Overlays", "Alert, menu, context menu, toast, loading"),
    Sheets("Sheets", "Detents, the card stack, non-modal and side sheets, materials, action and choice sheets"),
    Feedback("Feedback", "States, banners, skeletons, spinners, progress, badges, avatars"),
    Loading("Loading screen", "Redacted rows under a large title: one sweep, and the swap to content"),
    Feed("Photo feed", "Full-width photos over the network, at their own aspect ratios, fetched ahead"),
    Shop("Shop grid", "Product photos in two or three columns, scaled down to their tiles"),
    Reels("Short videos", "A reels feed of public-domain clips: muted autoplay on shared players"),
    Motion("Animation", "Transitions, first appearance, list placement, emphasis, deceleration, rubber band"),
    Adaptive("Adaptive", "Panes of any width, foldable postures, the hardware keyboard"),
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
        CatalogSection.Navigation -> NavigationSection()
        CatalogSection.Overlays -> OverlaysSection()
        CatalogSection.Sheets -> SheetsSection()
        CatalogSection.Feedback -> FeedbackSection()
        CatalogSection.Loading -> error("Loading is a whole screen, not a page of groups")
        CatalogSection.Feed, CatalogSection.Shop -> error("$name is a whole screen, not a page of groups")
        CatalogSection.Reels -> error("Reels is a whole screen, not a page of groups")
        CatalogSection.Motion -> MotionSection()
        CatalogSection.Adaptive -> AdaptiveSection()
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
