package io.example.fitcnet.core.designsystem.component.sheet

import androidx.compose.ui.unit.Dp

/**
 * The saved form of a sheet: `"<shown>|<key>"`, where shown is 1 or 0 and the key names the detent
 * it rested on. A custom detent is saved by its id and comes back only if the caller still lists it,
 * because its resolver cannot be saved.
 */
internal object SheetStateKey {
    fun encode(shown: Boolean, detent: SheetDetent?): String = "${if (shown) 1 else 0}|${detent?.let(::key).orEmpty()}"

    fun decode(saved: String, detents: List<SheetDetent>): Pair<Boolean, SheetDetent?> {
        val shown = saved.startsWith("1|")
        val key = saved.substringAfter('|', "")
        val detent = when {
            key == "large" -> SheetDetent.Large
            key == "medium" -> SheetDetent.Medium
            key == "fit" -> SheetDetent.FitContent
            key == "peek" -> SheetDetent.Peek
            key.startsWith("fraction:") -> key.removePrefix("fraction:").toFloatOrNull()
                ?.takeIf { it > 0f && it <= 1f }
                ?.let { SheetDetent.Fraction(it) }
            key.startsWith("height:") -> key.removePrefix("height:").toFloatOrNull()?.let { SheetDetent.Height(Dp(it)) }
            key.startsWith("custom:") -> key.removePrefix("custom:").let { id ->
                detents.firstOrNull { it is SheetDetent.Custom && it.id == id }
            }
            else -> null
        }
        return shown to detent
    }

    private fun key(detent: SheetDetent): String = when (detent) {
        SheetDetent.Large -> "large"
        SheetDetent.Medium -> "medium"
        SheetDetent.FitContent -> "fit"
        SheetDetent.Peek -> "peek"
        is SheetDetent.Fraction -> "fraction:${detent.fraction}"
        is SheetDetent.Height -> "height:${detent.height.value}"
        is SheetDetent.Custom -> "custom:${detent.id}"
    }
}
