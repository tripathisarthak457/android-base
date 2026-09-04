package com.base.app.core.common.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * A string that a ViewModel can produce without holding a `Context`.
 *
 * A ViewModel that resolves strings itself either keeps a Context — which leaks and breaks under
 * configuration change and locale switches — or returns raw English, which cannot be localised.
 * [UiText] defers resolution to the composable that renders it, so the same state object renders
 * correctly in every locale and survives the user changing theirs while the screen is open.
 *
 * [Resource] is the default. [Dynamic] exists for the one case a resource cannot cover: a message
 * that only the server knows.
 */
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
