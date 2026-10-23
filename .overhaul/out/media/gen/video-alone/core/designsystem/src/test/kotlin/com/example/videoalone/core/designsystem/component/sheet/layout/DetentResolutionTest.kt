package com.example.videoalone.core.designsystem.component.sheet.layout

import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.component.sheet.SheetDetent
import com.example.videoalone.core.designsystem.foundation.adaptive.SizeClass
import com.example.videoalone.core.designsystem.foundation.adaptive.WindowHeight
import com.example.videoalone.core.designsystem.foundation.adaptive.WindowWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** How detents resolve, at density 1 so pixels read as dp, in an 873 dp tall phone window. */
class DetentResolutionTest {

    private val phone = DetentInputs(
        boundsHeight = 873f,
        largeTop = 43f,
        restBottom = 857f,
        contentHeight = 300f,
        headerHeight = 72f,
        sizeClass = SizeClass.Phone,
        foldBottom = null,
        density = 1f,
        fontScale = 1f,
    )

    @Test
    fun `R-1 and R-2 Large sits below the safe top and Medium's top at half the window`() {
        val stops = resolveStops(listOf(SheetDetent.Medium, SheetDetent.Large), phone)
        assertEquals(listOf(SheetDetent.Large, SheetDetent.Medium), stops.detents)
        assertEquals(43f, stops.offsetOf(SheetDetent.Large), 0f)
        assertEquals(436.5f, stops.offsetOf(SheetDetent.Medium), 0f)
        assertEquals(873f, stops.hidden, 0f)
    }

    @Test
    fun `R-2 Medium is inactive in compact height and within 48dp of Large`() {
        val short = phone.copy(sizeClass = SizeClass(WindowWidth.Compact, WindowHeight.Compact))
        assertEquals(listOf(SheetDetent.Large), resolveStops(listOf(SheetDetent.Medium, SheetDetent.Large), short).detents)
        val tiny = phone.copy(boundsHeight = 120f, restBottom = 110f, largeTop = 20f)
        assertFalse(resolveStops(listOf(SheetDetent.Medium, SheetDetent.Large), tiny).has(SheetDetent.Medium))
    }

    @Test
    fun `R-2 in tabletop Medium rests on the fold`() {
        val stops = resolveStops(listOf(SheetDetent.Medium, SheetDetent.Large), phone.copy(foldBottom = 470f))
        assertEquals(470f, stops.offsetOf(SheetDetent.Medium), 0f)
    }

    @Test
    fun `R-3 R-3a and R-4 heights rise from the resting bottom`() {
        val stops = resolveStops(
            listOf(SheetDetent.FitContent, SheetDetent.Peek, SheetDetent.Fraction(0.5f), SheetDetent.Height(96.dp)),
            phone,
        )
        assertEquals(857f - 300f, stops.offsetOf(SheetDetent.FitContent), 0f)
        assertEquals(857f - 72f, stops.offsetOf(SheetDetent.Peek), 0f)
        assertEquals(857f - 0.5f * (857f - 43f), stops.offsetOf(SheetDetent.Fraction(0.5f)), 0.01f)
        assertEquals(857f - 96f, stops.offsetOf(SheetDetent.Height(96.dp)), 0f)
    }

    @Test
    fun `R-3a a peek without a header is the minimum height`() {
        val stops = resolveStops(listOf(SheetDetent.Peek, SheetDetent.Large), phone.copy(headerHeight = 0f))
        assertEquals(857f - 48f, stops.offsetOf(SheetDetent.Peek), 0f)
    }

    @Test
    fun `R-4 a custom resolver reads the context and may leave its detent out`() {
        val sliver = SheetDetent.Custom("sliver") { maxOf(44.dp, maxHeight * 0.1f) }
        val stops = resolveStops(listOf(sliver, SheetDetent.Large), phone)
        assertEquals(857f - 81.4f, stops.offsetOf(sliver), 0.01f)
        val absent = SheetDetent.Custom("absent") { null }
        assertEquals(listOf(SheetDetent.Large), resolveStops(listOf(absent, SheetDetent.Large), phone).detents)
        val half = SheetDetent.Custom("half") { heightOf(SheetDetent.Medium) }
        assertEquals(436.5f, resolveStops(listOf(half), phone).offsetOf(half), 0.01f)
    }

    @Test
    fun `R-5 every height is clamped between 48dp and Large`() {
        val stops = resolveStops(listOf(SheetDetent.Height(10.dp), SheetDetent.Height(5000.dp)), phone)
        assertEquals(857f - 48f, stops.offsetOf(SheetDetent.Height(10.dp)), 0f)
        assertEquals(43f, stops.offsetOf(SheetDetent.Height(5000.dp)), 0f)
    }

    @Test
    fun `R-6 detents within a pixel merge, and Large's identity wins`() {
        val full = phone.copy(contentHeight = 5000f)
        assertEquals(listOf(SheetDetent.Large), resolveStops(listOf(SheetDetent.FitContent, SheetDetent.Large), full).detents)
        val twins = resolveStops(listOf(SheetDetent.Height(96.dp), SheetDetent.Fraction(96f / 814f)), phone)
        assertEquals(listOf(SheetDetent.Height(96.dp)), twins.detents)
    }

    @Test
    fun `R-6a a stop as tall as Large counts as Large whatever produced it`() {
        val full = phone.copy(contentHeight = 5000f)
        val alone = resolveStops(listOf(SheetDetent.FitContent), full)
        assertEquals(0, alone.largeIndex)
        val first = resolveStops(listOf(SheetDetent.FitContent, SheetDetent.Large), full)
        val second = resolveStops(listOf(SheetDetent.Large, SheetDetent.FitContent), full)
        assertEquals(first.offsets.toList(), alone.offsets.toList())
        assertEquals(first, second)
        assertEquals(-1, resolveStops(listOf(SheetDetent.Medium), phone).largeIndex)
    }

    @Test
    fun `R-7 stops are ordered by offset whatever the list order`() {
        val stops = resolveStops(listOf(SheetDetent.Height(96.dp), SheetDetent.Large, SheetDetent.Medium), phone)
        assertEquals(listOf(SheetDetent.Large, SheetDetent.Medium, SheetDetent.Height(96.dp)), stops.detents)
        assertTrue(stops.offsets.toList() == stops.offsets.sorted())
    }

    @Test
    fun `R-8 a sheet whose detents are all inactive gets Large`() {
        val short = phone.copy(sizeClass = SizeClass(WindowWidth.Expanded, WindowHeight.Compact))
        val stops = resolveStops(listOf(SheetDetent.Medium, SheetDetent.Custom("none") { null }), short)
        assertEquals(listOf(SheetDetent.Large), stops.detents)
    }

    @Test
    fun `R-9 an inactive detent maps to the active stop nearest it by height`() {
        val short = phone.copy(sizeClass = SizeClass(WindowWidth.Compact, WindowHeight.Compact))
        val stops = resolveStops(listOf(SheetDetent.Medium, SheetDetent.Height(96.dp), SheetDetent.Large), short)
        val wouldBe = nominalOffset(SheetDetent.Medium, short)!!
        assertEquals(436.5f, wouldBe, 0f)
        assertEquals(SheetDetent.Height(96.dp), stops.detents[stops.nearestIndex(wouldBe + 200f)])
        assertEquals(SheetDetent.Large, stops.detents[stops.nearestIndex(wouldBe - 300f)])
    }

    @Test
    fun `R-10 a form sheet or side panel has one stop carrying its recorded detent`() {
        val stops = singleStop(SheetDetent.Medium, rest = 300f, hidden = 1280f, roomAbove = 276f, span = 1280f, inputs = phone)
        assertTrue(stops.single)
        assertEquals(listOf(SheetDetent.Medium), stops.detents)
        assertEquals(300f, stops.top, 0f)
        assertEquals(-1, stops.largeIndex)
    }

    @Test
    fun `R-12 no partial stop straddles a tabletop crease, but Large may`() {
        val folded = phone.copy(foldBottom = 470f)
        val stops = resolveStops(listOf(SheetDetent.Fraction(0.75f), SheetDetent.Large), folded)
        assertEquals(470f, stops.offsetOf(SheetDetent.Fraction(0.75f)), 0f)
        assertEquals(43f, stops.offsetOf(SheetDetent.Large), 0f)
    }

    @Test
    fun `R-13 upThrough maps to itself, else to the highest active stop no taller, else the lowest`() {
        val stops = resolveStops(listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large), phone)
        assertEquals(1, boundaryIndex(SheetDetent.Medium, stops, phone))
        val short = phone.copy(boundsHeight = 400f, restBottom = 384f, largeTop = 20f, sizeClass = SizeClass(WindowWidth.Medium, WindowHeight.Compact))
        val shortStops = resolveStops(listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large), short)
        assertEquals(listOf(SheetDetent.Large, SheetDetent.Peek), shortStops.detents)
        assertEquals(SheetDetent.Peek, shortStops.detents[boundaryIndex(SheetDetent.Medium, shortStops, short)])
    }

    @Test
    fun `a non-modal sheet's heights rise from the tab bar's settled top`() {
        val aboveBar = phone.copy(restBottom = 873f - 80f)
        assertEquals(793f - 96f, resolveStops(listOf(SheetDetent.Height(96.dp)), aboveBar).offsetOf(SheetDetent.Height(96.dp)), 0f)
    }

    @Test
    fun `B-54 a stacked sheet's Large is the covered sheet's lifted top plus the peek`() {
        val coveredTop = 43f - 4.4f
        val stacked = phone.copy(largeTop = coveredTop + 10f)
        assertEquals(coveredTop + 10f, resolveStops(listOf(SheetDetent.Large), stacked).top, 0f)
    }

    @Test
    fun `B-24 the band above reaches Large's stretch line`() {
        assertEquals(0.008f * 873f, resolveStops(listOf(SheetDetent.Large), phone).roomAbove, 0.01f)
        val medium = resolveStops(listOf(SheetDetent.Medium), phone)
        assertEquals(436.5f - (43f - 0.008f * 873f), medium.roomAbove, 0.01f)
    }

    @Test
    fun `equal stops are equal, so anchors are written only when they change`() {
        val a = resolveStops(listOf(SheetDetent.Medium, SheetDetent.Large), phone)
        val b = resolveStops(listOf(SheetDetent.Large, SheetDetent.Medium), phone)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == resolveStops(listOf(SheetDetent.Large), phone))
    }
}
