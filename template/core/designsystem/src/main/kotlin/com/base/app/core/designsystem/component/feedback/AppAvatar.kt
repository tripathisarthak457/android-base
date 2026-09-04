package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.absoluteValue

/**
 * An initials avatar, with a colour derived from the name.
 *
 * The colour comes from a hash of the name rather than being random or always the accent, so the
 * same person is the same colour on every screen and across sessions. In a list of names that
 * consistency is what makes an avatar useful at a glance rather than decorative.
 *
 * Image avatars live in `:core:ui` — loading one needs an image library, and this module
 * deliberately has no dependencies beyond Compose. Pass [content] to place any composable inside
 * the same circle; that is the seam `:core:ui` uses.
 */
@Composable
fun AppAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: @Composable (() -> Unit)? = null,
) {
    val palette = AppTheme.avatarPalette()
    val background = palette[name.stableIndex(palette.size)]

    AppSurface(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = name },
        shape = AppTheme.shapes.pill,
        color = background,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (content != null) {
                content()
            } else {
                AppText(
                    text = name.initials(),
                    style = AppTheme.typography.titleSmall,
                    color = AppTheme.colors.contentPrimary,
                )
            }
        }
    }
}

/**
 * The first letter of the first word and of the last, upper-cased.
 *
 * Words are split on whitespace and blanks are dropped, so a double space or a trailing one does
 * not produce an initial that is a space character — which renders as an empty circle and looks
 * like a loading failure.
 */
private fun String.initials(): String {
    val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when (words.size) {
        0 -> "?"
        1 -> words[0].take(2).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}

/**
 * A stable index for this string.
 *
 * `String.hashCode` is specified by the language, not by the JVM implementation, so the same name
 * lands on the same colour on every device and every release — which a hash chosen by the runtime
 * would not guarantee.
 */
private fun String.stableIndex(bound: Int): Int =
    if (bound <= 0) 0 else hashCode().absoluteValue % bound

/**
 * Muted tints for avatar backgrounds.
 *
 * Drawn from the status washes rather than a separate palette: they are already tuned for both
 * themes and already guaranteed to carry the primary content colour legibly.
 */
@Composable
private fun AppTheme.avatarPalette(): List<Color> = listOf(
    colors.info.subtle,
    colors.success.subtle,
    colors.warning.subtle,
    colors.accentSubtle,
    colors.neutral.subtle,
    colors.danger.subtle,
)
