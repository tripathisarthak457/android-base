package com.base.app.core.designsystem.component.sheet.choice

import java.text.Normalizer
import java.util.Locale

/**
 * The choice sheet's search: case and accents folded on both sides, so "espanol" finds "Español" and
 * "ZURICH" finds "Zürich". The label and the supporting text both count.
 */
internal object ChoiceSearch {
    fun fold(text: String, locale: Locale): String =
        Combining.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(locale)

    fun matches(query: String, label: String, supporting: String?, locale: Locale): Boolean {
        val wanted = fold(query.trim(), locale)
        if (wanted.isEmpty()) return true
        return fold(label, locale).contains(wanted) || (supporting != null && fold(supporting, locale).contains(wanted))
    }

    private val Combining = Regex("\\p{Mn}+")
}
