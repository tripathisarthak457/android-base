package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.theme.AppColors
import com.base.app.core.designsystem.theme.AppTheme
import kotlin.math.absoluteValue

/**
 * An avatar: [content] (an image) when there is one, otherwise the name's initials on a colour the
 * name always maps to. A screen reader hears the name once, not the initials as well.
 */
@Composable
fun AppAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val initials = remember(name) { name.initials() }
    val tint = remember(name, colors) { avatarTint(name, colors) }
    AppSurface(
        modifier = modifier.size(size).clearAndSetSemantics { contentDescription = name },
        shape = AppTheme.shapes.pill,
        color = tint,
        clipContent = content != null,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (content != null) {
                content()
            } else {
                AppText(text = initials, style = AppTheme.typography.footnote, color = colors.contentPrimary)
            }
        }
    }
}

/**
 * Overlapping avatars for the people in something, the first few shown and the rest counted:
 * "+3". Each one is ringed in the background colour so the overlap reads as a stack.
 */
@Composable
fun AppAvatarGroup(
    names: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    maxShown: Int = 4,
) {
    val colors = AppTheme.colors
    val shown = names.take(maxShown)
    val hidden = names.size - shown.size
    val description = pluralStringResource(R.plurals.designsystem_people_count, names.size, names.size)
    val ring = 2.dp
    Layout(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        content = {
            shown.forEach { name ->
                AppAvatar(name = name, size = size, modifier = Modifier.border(ring, colors.background, AppTheme.shapes.pill))
            }
            if (hidden > 0) {
                AppSurface(
                    modifier = Modifier.size(size).border(ring, colors.background, AppTheme.shapes.pill),
                    shape = AppTheme.shapes.pill,
                    color = colors.fill,
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(text = "+$hidden", style = AppTheme.typography.caption1, color = colors.contentPrimary)
                }
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val step = (size * OVERLAP_STEP).roundToPx()
        val width = if (placeables.isEmpty()) 0 else step * (placeables.size - 1) + placeables.last().width
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(step * index, 0) }
        }
    }
}

private val Whitespace = Regex("\\s+")

/** The first letter of the first word and of the last, upper-cased. */
private fun String.initials(): String {
    val words = trim().split(Whitespace).filter { it.isNotBlank() }
    return when (words.size) {
        0 -> "?"
        1 -> words[0].take(2).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}

/** One of the tinted fills, always the same one for the same name. */
private fun avatarTint(name: String, colors: AppColors): Color {
    val options = listOf(colors.accentSubtle, colors.secondarySubtle, colors.tertiarySubtle, colors.success.subtle, colors.warning.subtle, colors.info.subtle)
    return options[(name.hashCode().absoluteValue) % options.size]
}

private const val OVERLAP_STEP = 0.7f
