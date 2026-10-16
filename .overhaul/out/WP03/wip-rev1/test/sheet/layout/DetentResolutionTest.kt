package com.base.app.core.designsystem.component.sheet.layout

import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.foundation.adaptive.SizeClass
import com.base.app.core.designsystem.foundation.adaptive.WindowHeight
import com.base.app.core.designsystem.foundation.adaptive.WindowWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** R-1 to R-11 of the sheet specification, at density 1 so pixels read as dp. */
class DetentResolutionTest {

    private val phone = DetentInputs(
        boundsHeight = 873f,
        safeTop = 33f,
        largeTop = 43f,
        restBottom = 857f,
        contentHeight = 300f,
        sizeClass = SizeClass.Phone,
        foldBottom = null,
        density = 1f,
        fontScale = 1f,
    )

    @Test
    fun `R-1 and R-2 Large sits below the safe top and Medium at half the window`() {
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
    fun `R-3 and R-4 heights rise from the resting bottom`() {
        val stops = resolveStops(
            listOf(SheetDetent.FitContent, SheetDetent.Fraction(0.5f), SheetDetent.Height(96.dp)),
            phone,
        )
        assertEquals(857f - 300f, stops.offsetOf(SheetDetent.FitContent), 0f)
        assertEquals(857f - 0.5f * (857f - 43f), stops.offsetOf(SheetDetent.Fraction(0.5f)), 0.01f)
        assertEquals(857f - 96f, stops.offsetOf(SheetDetent.Height(96.dp)), 0f)
    }

    @Test
    fun `R-4 a custom resolver reads the context and may leave its detent out`() {
        val sliver = SheetDetent.Custom("sliver") { maxOf(44.dp, maxHeight * 0.1f) }
        val stops = resolveStops(listOf(sliver, SheetDetent.Large), phone)
        assertEquals(857f - 81.4f, stops.offsetOf(sliver), 0.01f)
        val absent = SheetDetent.Custom("absent") { null }
        assertEquals(listOf(SheetDetent.Large), resolveStops(listOf(absent, SheetDetent.Large), phone).detents)
        val half = SheetDetent.Custom("half") { heightOf(SheetDetent.Medium) }
        assertEquals(436.5f, resolveStops(listOf(half), phone).offsetOf(half), 0f)
    }

    @Test
    fun `R-5 every height is clamped between 48dp and Large`() {
        val stops = resolveStops(listOf(SheetDetent.Height(10.dp), SheetDetent.Height(5000.dp)), phone)
        assertEquals(857f - 48f, stops.offsetOf(SheetDetent.Height(10.dp)), 0f)
        assertEquals(43f, stops.offsetOf(SheetDetent.Height(5000.dp)), 0f)
    }

    @Test
    fun `R-6 detents within a pixel merge into the first listed`() {
        val tall = phone.copy(contentHeight = 5000f)
        val stops = resolveStops(listOf(SheetDetent.FitContent, SheetDetent.Large), tall)
        assertEquals(listOf(SheetDetent.FitContent), stops.detents)
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
    fun `R-10 a form sheet has one centred stop no taller than 620dp or 85 percent`() {
        val tablet = phone.copy(boundsHeight = 1280f, safeTop = 24f, largeTop = 34f, restBottom = 1256f, contentHeight = 2000f)
        val stops = resolveFormStop(SheetDetent.Medium, tablet)
        assertEquals(listOf(SheetDetent.Medium), stops.detents)
        assertEquals(620f, stops.formHeight, 0f)
        assertEquals(24f + (1232f - 620f) / 2f, stops.top, 0f)
        val short = resolveFormStop(SheetDetent.Large, tablet.copy(contentHeight = 200f))
        assertEquals(200f, short.formHeight, 0f)
    }

    @Test
    fun `a non-modal sheet's heights rise from the top of the bottom chrome`() {
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
    fun `equal stops are equal, so anchors are written only when they change`() {
        val a = resolveStops(listOf(SheetDetent.Medium, SheetDetent.Large), phone)
        val b = resolveStops(listOf(SheetDetent.Large, SheetDetent.Medium), phone)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == resolveStops(listOf(SheetDetent.Large), phone))
    }

    @Test
    fun `a detent no longer listed maps to the nearest active stop`() {
        val stops = resolveStops(listOf(SheetDetent.Height(96.dp), SheetDetent.Large), phone)
        val wouldBe = offsetOf(SheetDetent.Medium, phone)!!
        assertEquals(SheetDetent.Height(96.dp), stops.nearest(wouldBe + 200f))
        assertEquals(SheetDetent.Large, stops.nearest(wouldBe - 200f))
    }
}
