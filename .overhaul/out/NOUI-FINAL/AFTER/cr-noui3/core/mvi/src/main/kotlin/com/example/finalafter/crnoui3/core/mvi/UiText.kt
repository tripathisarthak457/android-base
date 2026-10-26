package com.example.finalafter.crnoui3.core.mvi

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/** A string that a ViewModel can produce without holding a `Context`. */
sealed interface UiText {

    /** Words that arrived from outside, such as a server's message. Never the app's own copy. */
    data class Dynamic(val value: String) : UiText

    data class Resource(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    /** A count, worded for the current language's plural rules: "1 character", "8 characters". */
    data class Plural(
        @param:PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = listOf(count),
    ) : UiText

    fun resolve(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args.toTypedArray())
        is Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())

        fun plural(@PluralsRes id: Int, count: Int): UiText = Plural(id, count)
    }
}
