package com.base.app.core.common.util

import android.content.Context
import androidx.annotation.StringRes

/** A string that a ViewModel can produce without holding a `Context`. */
sealed interface UiText {

    data class Dynamic(val value: String) : UiText

    data class Resource(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    /** Concatenation, for a label assembled from parts that resolve differently. */
    data class Composite(val parts: List<UiText>, val separator: String = " ") : UiText

    fun resolve(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args.toTypedArray())
        is Composite -> parts.joinToString(separator) { it.resolve(context) }
    }

    companion object {
        val Empty: UiText = Dynamic("")

        fun of(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())
    }
}

fun String.asUiText(): UiText = UiText.Dynamic(this)

fun String?.orUiText(fallback: UiText): UiText =
    if (isNullOrBlank()) fallback else UiText.Dynamic(this)
