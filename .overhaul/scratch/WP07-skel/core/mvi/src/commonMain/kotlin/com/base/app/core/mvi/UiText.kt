package com.base.app.core.mvi

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

/** A string that a ViewModel can produce without knowing the language it will be shown in. */
sealed interface UiText {

    /** Words that arrived from outside, such as a server's message. Never the app's own copy. */
    data class Dynamic(val value: String) : UiText

    data class Resource(
        val id: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText

    /** A count, worded for the current language's plural rules: "1 character", "8 characters". */
    data class Plural(
        val id: PluralStringResource,
        val count: Int,
        val args: List<Any> = listOf(count),
    ) : UiText

    companion object {
        fun of(id: StringResource, vararg args: Any): UiText = Resource(id, args.toList())

        fun plural(id: PluralStringResource, count: Int): UiText = Plural(id, count)
    }
}
