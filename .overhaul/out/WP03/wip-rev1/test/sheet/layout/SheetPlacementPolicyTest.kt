package com.base.app.core.designsystem.component.sheet.layout

import com.base.app.core.designsystem.component.sheet.SheetPlacement
import com.base.app.core.designsystem.foundation.adaptive.SizeClass
import com.base.app.core.designsystem.foundation.adaptive.WindowHeight
import com.base.app.core.designsystem.foundation.adaptive.WindowPosture
import com.base.app.core.designsystem.foundation.adaptive.WindowWidth
import org.junit.Assert.assertEquals
import org.junit.Test

/** §8's table: where Automatic puts a sheet, and when an explicit placement falls back. */
class SheetPlacementPolicyTest {

    private val phone = SizeClass.Phone
    private val sideways = SizeClass(WindowWidth.Expanded, WindowHeight.Compact)
    private val smallTablet = SizeClass(WindowWidth.Medium, WindowHeight.Expanded)
    private val tablet = SizeClass(WindowWidth.Expanded, WindowHeight.Medium)

    private fun automatic(modal: Boolean, sizeClass: SizeClass, posture: WindowPosture.Kind = WindowPosture.Kind.Flat) =
        resolvePlacement(SheetPlacement.Automatic, modal, sizeClass, posture)

    @Test
    fun `a phone shows every sheet from the bottom`() {
        assertEquals(SheetPlacement.Bottom, automatic(modal = true, phone))
        assertEquals(SheetPlacement.Bottom, automatic(modal = false, phone))
    }

    @Test
    fun `a short window shows every sheet from the bottom`() {
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
    fun `tabletop keeps sheets off the crease and book posture keeps them in a half`() {
        assertEquals(SheetPlacement.Bottom, automatic(modal = true, tablet, WindowPosture.Kind.Tabletop))
        assertEquals(SheetPlacement.Center, automatic(modal = true, tablet, WindowPosture.Kind.Book))
        assertEquals(SheetPlacement.Start, automatic(modal = false, tablet, WindowPosture.Kind.Book))
    }

    @Test
    fun `explicit placements are honoured, and the sides need room`() {
        assertEquals(SheetPlacement.Center, resolvePlacement(SheetPlacement.Center, true, phone, WindowPosture.Kind.Flat))
        assertEquals(SheetPlacement.Bottom, resolvePlacement(SheetPlacement.Bottom, true, tablet, WindowPosture.Kind.Flat))
        assertEquals(SheetPlacement.Bottom, resolvePlacement(SheetPlacement.End, true, smallTablet, WindowPosture.Kind.Flat))
        assertEquals(SheetPlacement.End, resolvePlacement(SheetPlacement.End, false, tablet, WindowPosture.Kind.Flat))
    }
}
