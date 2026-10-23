package com.example.lean.core.designsystem.component.sheet.choice

import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The choice sheet's search folds case and accents on both sides and reads the supporting text. */
class ChoiceSearchTest {

    private val locale = Locale.ENGLISH

    @Test
    fun `case and accents are folded on both sides`() {
        assertTrue(ChoiceSearch.matches("espanol", "Español", null, locale))
        assertTrue(ChoiceSearch.matches("ESPAÑOL", "español", null, locale))
        assertTrue(ChoiceSearch.matches("zurich", "Zürich", null, locale))
    }

    @Test
    fun `the supporting text matches too`() {
        assertTrue(ChoiceSearch.matches("spanish", "Español", "Spanish", locale))
        assertFalse(ChoiceSearch.matches("german", "Español", "Spanish", locale))
    }

    @Test
    fun `an empty query matches everything`() {
        assertTrue(ChoiceSearch.matches("  ", "Deutsch", null, locale))
    }
}
