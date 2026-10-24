package io.example.audit.core.designsystem.component.sheet

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** B-62: what a sheet saves across recreation and process death, and what comes back. */
class SheetSaverTest {

    private val custom = SheetDetent.Custom("sliver") { 44.dp }
    private val listed = listOf(
        SheetDetent.Large,
        SheetDetent.Medium,
        SheetDetent.FitContent,
        SheetDetent.Peek,
        SheetDetent.Fraction(0.25f),
        SheetDetent.Height(96.dp),
        custom,
    )

    @Test
    fun `B-62 every detent round-trips with whether the sheet was shown`() {
        for (detent in listed) {
            for (shown in listOf(true, false)) {
                assertEquals(shown to detent, SheetStateKey.decode(SheetStateKey.encode(shown, detent), listed))
            }
        }
    }

    @Test
    fun `B-62 a custom detent the caller no longer lists comes back as nothing`() {
        val saved = SheetStateKey.encode(true, custom)
        assertNull(SheetStateKey.decode(saved, listOf(SheetDetent.Large)).second)
    }

    @Test
    fun `a hidden sheet saves no detent`() {
        assertEquals(false to null, SheetStateKey.decode(SheetStateKey.encode(false, null), listed))
    }

    @Test
    fun `B-6 a restored shown sheet arrives at its detent, a restored hidden one at nothing`() {
        val saver = AppSheetState.Saver(listOf(SheetDetent.Medium, SheetDetent.Large))
        val shown = saver.restore("1|large")!!
        assertEquals(SheetDetent.Large, shown.restoredDetent)
        val hidden = saver.restore("0|medium")!!
        assertNull(hidden.restoredDetent)
    }
}
