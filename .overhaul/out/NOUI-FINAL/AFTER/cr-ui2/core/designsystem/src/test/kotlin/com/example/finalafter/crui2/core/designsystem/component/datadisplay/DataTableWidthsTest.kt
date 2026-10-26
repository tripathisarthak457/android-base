package com.example.finalafter.crui2.core.designsystem.component.datadisplay

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DataTableWidthsTest {

    private data class Row(val name: String, val amount: String)

    private val columns = listOf(
        AppTableColumn<Row>("Name", AppColumnType.Text) { it.name },
        AppTableColumn("Amount", AppColumnType.Money, total = "12,345.00") { it.amount },
    )

    @Test
    fun `only figure columns are measured, by their longest value or total`() {
        val rows = listOf(Row("A", "1.00"), Row("Somebody long", "999.00"))
        assertEquals(listOf(null, "12,345.00"), longestFigures(columns, rows))
    }

    @Test
    fun `a column grows to fit its widest figure and never shrinks below its own width`() {
        assertEquals(listOf(160.dp, 140.dp), widenedToFit(listOf(160.dp, 124.dp), listOf(null, 140.dp)))
        assertEquals(listOf(160.dp, 124.dp), widenedToFit(listOf(160.dp, 124.dp), listOf(null, 100.dp)))
    }

    @Test
    fun `columns that overflow keep their widths, and spare room is shared in proportion`() {
        assertEquals(listOf(200.dp, 200.dp), fittedWidths(listOf(200.dp, 200.dp), 300.dp))
        assertEquals(listOf(200.dp, 400.dp), fittedWidths(listOf(100.dp, 200.dp), 600.dp))
    }
}
