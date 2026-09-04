package com.base.app.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.data.AppBarChart
import com.base.app.core.designsystem.component.data.AppDetailRow
import com.base.app.core.designsystem.component.data.AppKeyValueGrid
import com.base.app.core.designsystem.component.data.AppRating
import com.base.app.core.designsystem.component.data.AppSectionHeader
import com.base.app.core.designsystem.component.data.AppSparkline
import com.base.app.core.designsystem.component.data.AppStatTile
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Read-only presentation: the components a details screen, a receipt or a dashboard is built from.
 *
 * The two charts are drawn on a `Canvas` rather than pulled from a charting library. Anything more
 * than a trend line or a ranked bar belongs to a real library — these exist so the common case
 * does not drag one in.
 */
@Composable
fun DataSection() {
    var rating by remember { mutableIntStateOf(4) }

    CatalogGroup(title = "Section header", caption = "With an optional action on the right.") {
        AppSectionHeader(
            title = "Recent orders",
            subtitle = "Last 30 days",
            actionLabel = "See all",
            onAction = {},
        )
    }

    CatalogGroup(title = "Detail rows", caption = "Label left, value right; mono for identifiers.") {
        AppCard {
            AppDetailRow(label = "Status", value = "Delivered", icon = AppIcons.Check)
            AppDetailRow(label = "Order ID", value = "8F3K-2211-90XZ", mono = true)
            AppDetailRow(
                label = "Total",
                value = "₹1,250.00",
                valueColor = AppTheme.colors.contentPrimary,
            )
            AppDetailRow(label = "Invoice", value = "Download", onClick = {})
        }
    }

    CatalogGroup(title = "Stat tiles", caption = "A number, and whether it moved the right way.") {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppStatTile(
                label = "Revenue",
                value = "₹4.2L",
                delta = "+12.4%",
                icon = AppIcons.ArrowUp,
                modifier = Modifier.weight(1f),
            )
            AppStatTile(
                label = "Refunds",
                value = "₹18k",
                delta = "-3.1%",
                deltaIsPositive = false,
                modifier = Modifier.weight(1f),
            )
        }
    }

    CatalogGroup(title = "Rating", caption = "Read-only above, tappable below.") {
        AppRating(rating = 3.5f)
        AppRating(rating = rating.toFloat(), onRatingChange = { rating = it })
        AppText(
            text = "You rated this $rating",
            style = AppTheme.typography.caption,
            color = AppTheme.colors.contentTertiary,
        )
    }

    CatalogGroup(title = "Sparkline", caption = "Draws itself in once, then stays still.") {
        AppCard {
            AppSparkline(
                values = SparkValues,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    CatalogGroup(title = "Bar chart", caption = "Ranked comparison, labelled in place.") {
        AppCard {
            AppBarChart(
                entries = listOf(
                    "Direct" to 480f,
                    "Search" to 310f,
                    "Social" to 190f,
                    "Email" to 90f,
                ),
            )
        }
    }

    CatalogGroup(title = "Key-value grid", caption = "For specifications and metadata.") {
        AppCard {
            AppKeyValueGrid(
                entries = listOf(
                    "Model" to "Pixel 9 Pro",
                    "Storage" to "256 GB",
                    "Colour" to "Obsidian",
                    "Warranty" to "Until Mar 2027",
                ),
            )
        }
    }
}

private val SparkValues = listOf(
    12f, 18f, 15f, 24f, 22f, 31f, 28f, 36f, 42f, 39f, 48f, 61f,
)
