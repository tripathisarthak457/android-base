package com.base.app.catalog.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.catalog.CatalogGroup
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.data.AppAnimatedCounter
import com.base.app.core.designsystem.component.data.AppBarChart
import com.base.app.core.designsystem.component.data.AppColumnType
import com.base.app.core.designsystem.component.data.AppDataTable
import com.base.app.core.designsystem.component.data.AppKeyValueGrid
import com.base.app.core.designsystem.component.data.AppRating
import com.base.app.core.designsystem.component.data.AppSparkline
import com.base.app.core.designsystem.component.data.AppStatTile
import com.base.app.core.designsystem.component.data.AppTableColumn
import com.base.app.core.designsystem.component.data.AppTimeline
import com.base.app.core.designsystem.component.data.TimelineStep

@Composable
fun DataSection() {
    CatalogGroup("Stats and charts") {
        AppStatTile(label = "Revenue", value = "$12,480", delta = "+8.2%")
        AppSparkline(values = listOf(3f, 5f, 4f, 8f, 6f, 9f, 12f), modifier = Modifier.fillMaxWidth())
        AppBarChart(entries = listOf("Mon" to 4f, "Tue" to 7f, "Wed" to 3f, "Thu" to 9f, "Fri" to 6f))
    }
    CatalogGroup("Key values") {
        AppKeyValueGrid(entries = listOf("Order" to "#4021", "Placed" to "9 Oct 2026", "Items" to "3", "Total" to "$84.00"))
    }
    CatalogGroup("Timeline", caption = "Each connector reaches the next node whatever the step's text wraps to.") {
        AppTimeline(
            steps = listOf(
                TimelineStep("Ordered", "We have your order.", "9 Oct, 10:02"),
                TimelineStep("Packed", "Packed at the warehouse in Leeds, with a description long enough to wrap.", "9 Oct, 14:40"),
                TimelineStep("Out for delivery"),
                TimelineStep("Delivered"),
            ),
            currentIndex = 2,
        )
    }
    CatalogGroup("Table", caption = "Columns scroll sideways together under a header that stays on top; figures line up.") {
        data class Trip(val date: String, val route: String, val km: String, val fare: String)
        AppDataTable(
            columns = listOf(
                AppTableColumn<Trip>("Date", AppColumnType.Date) { it.date },
                AppTableColumn("Route", AppColumnType.Text) { it.route },
                AppTableColumn("Km", AppColumnType.Number, total = "1,182.4") { it.km },
                AppTableColumn("Fare", AppColumnType.Money, total = "$2,409.10") { it.fare },
            ),
            rows = (1..20).map { Trip("0$it-10-2026".takeLast(10), "Leeds to York", "%.1f".format(40f + it * 3.3f), "$%.2f".format(80f + it * 7.1f)) },
            rowKey = { it.date + it.fare },
            onRowClick = {},
            modifier = Modifier.fillMaxWidth().height(320.dp),
        )
    }
    CatalogGroup("Counter and rating") {
        var count by remember { mutableLongStateOf(1_240) }
        AppAnimatedCounter(count = count)
        AppButton(text = "Add 17", onClick = { count += 17 }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
        var stars by remember { mutableFloatStateOf(3.5f) }
        AppRating(rating = stars, onRatingChange = { stars = it.toFloat() })
    }
}
