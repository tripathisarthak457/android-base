package io.example.audit.core.designsystem.component.sheet.layout

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.LayoutDirection
import io.example.audit.core.designsystem.component.sheet.SheetPlacement
import io.example.audit.core.designsystem.foundation.adaptive.SizeClass
import io.example.audit.core.designsystem.foundation.adaptive.WindowHeight
import io.example.audit.core.designsystem.foundation.adaptive.WindowWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** First match winning: where Automatic puts a sheet, and when an explicit placement falls back. */
class SheetPlacementPolicyTest {

    private val phone = SizeClass.Phone
    private val sideways = SizeClass(WindowWidth.Medium, WindowHeight.Compact)
    private val smallTablet = SizeClass(WindowWidth.Medium, WindowHeight.Expanded)
    private val tablet = SizeClass(WindowWidth.Expanded, WindowHeight.Medium)

    private fun automatic(modal: Boolean, sizeClass: SizeClass, tabletop: Boolean = false) =
        resolvePlacement(SheetPlacement.Automatic, modal, sizeClass, tabletop)

    @Test
    fun `a phone shows every sheet from the bottom`() {
        assertEquals(SheetPlacement.Bottom, automatic(modal = true, phone))
        assertEquals(SheetPlacement.Bottom, automatic(modal = false, phone))
    }

    @Test
    fun `a short window shows every sheet from the bottom, even at medium width (800 by 360)`() {
        assertEquals(SheetPlacement.Bottom, automatic(modal = true, sideways))
        assertEquals(SheetPlacement.Bottom, automatic(modal = false, sideways))
    }

    @Test
    fun `a modal sheet is a form sheet from medium width`() {
        assertEquals(SheetPlacement.Center, automatic(modal = true, smallTablet))
        assertEquals(SheetPlacement.Center, automatic(modal = true, tablet))
    }

    @Test
    fun `a non-modal sheet is a start panel from expanded width and a bottom sheet below it`() {
        assertEquals(SheetPlacement.Bottom, automatic(modal = false, smallTablet))
        assertEquals(SheetPlacement.Start, automatic(modal = false, tablet))
    }

    @Test
    fun `a tabletop crease keeps automatic sheets at the bottom`() {
        assertEquals(SheetPlacement.Bottom, automatic(modal = true, tablet, tabletop = true))
    }

    @Test
    fun `explicit placements are honoured, and the sides need an expanded width`() {
        assertEquals(SheetPlacement.Center, resolvePlacement(SheetPlacement.Center, true, phone, false))
        assertEquals(SheetPlacement.Bottom, resolvePlacement(SheetPlacement.Bottom, true, tablet, false))
        assertEquals(SheetPlacement.Bottom, resolvePlacement(SheetPlacement.End, true, smallTablet, false))
        assertEquals(SheetPlacement.End, resolvePlacement(SheetPlacement.End, true, tablet, false))
        assertEquals(SheetPlacement.Start, resolvePlacement(SheetPlacement.Start, false, tablet, false))
    }

    @Test
    fun `a vertical hinge puts the sheet in the presenter's half, else the larger, else the end`() {
        val hinge = Rect(590f, 0f, 610f, 900f)
        assertEquals(Rect(0f, 0f, 590f, 900f), hingeHalf(1200f, 900f, hinge, presenterX = 100f, LayoutDirection.Ltr))
        assertEquals(Rect(610f, 0f, 1200f, 900f), hingeHalf(1200f, 900f, hinge, presenterX = 900f, LayoutDirection.Ltr))
        val offCentre = Rect(490f, 0f, 510f, 900f)
        assertEquals(Rect(510f, 0f, 1200f, 900f), hingeHalf(1200f, 900f, offCentre, presenterX = null, LayoutDirection.Ltr))
        assertEquals(Rect(610f, 0f, 1200f, 900f), hingeHalf(1200f, 900f, hinge, null, LayoutDirection.Ltr))
        assertEquals(Rect(0f, 0f, 590f, 900f), hingeHalf(1200f, 900f, hinge, null, LayoutDirection.Rtl))
    }

    @Test
    fun `a horizontal hinge splits nothing sideways and gives the fold's bottom`() {
        val fold = Rect(0f, 400f, 800f, 420f)
        assertNull(hingeHalf(800f, 900f, fold, null, LayoutDirection.Ltr))
        assertEquals(420f, foldBottom(fold, boundsTop = 0f, boundsHeight = 900f))
        assertNull(foldBottom(Rect(590f, 0f, 610f, 900f), boundsTop = 0f, boundsHeight = 900f))
    }
}
