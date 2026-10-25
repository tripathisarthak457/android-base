package com.base.app.core.designsystem.component.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.foundation.draw.ContentUnderBone
import com.base.app.core.designsystem.foundation.draw.LocalRedacted
import com.base.app.core.designsystem.foundation.draw.skeleton
import com.base.app.core.designsystem.preview.AppComponentPreviews
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
    // In proportion to the circle, as iOS's contact monogram, and deaf to the font scale: the
    // circle does not grow with it, so larger letters would only overflow it.
    val initialsSize = with(LocalDensity.current) { (size * InitialsFraction).toSp() }
    val bone = LocalRedacted.current
    val redacted = if (bone) Modifier.skeleton(CircleShape) else Modifier
    AppSurface(
        modifier = modifier.size(size).clearAndSetSemantics { contentDescription = name }.then(redacted),
        shape = AppTheme.shapes.pill,
        color = tint,
        clipContent = content != null,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (content != null) {
                // Still redacted: told it is not, an image would load only to stay hidden under the bone.
                content()
            } else {
                ContentUnderBone(bone) {
                    AppText(
                        text = initials,
                        style = AppTheme.typography.footnote.copy(fontSize = initialsSize, lineHeight = initialsSize),
                        color = colors.contentPrimary,
                        maxLines = 1,
                    )
                }
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
        val step = (size * OverlapStep).roundToPx()
        val width = if (placeables.isEmpty()) 0 else step * (placeables.size - 1) + placeables.last().width
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(step * index, 0) }
        }
    }
}

private val Whitespace = Regex("\\s+")

/**
 * The first letters of the first and the last word that start with a letter, upper-cased: "Ada
 * Lovelace" is "AL", "Author 10" is "A", a single word one letter.
 */
private fun String.initials(): String {
    val words = trim().split(Whitespace).filter { it.firstOrNull()?.isLetter() == true }
    return when (words.size) {
        0 -> "?"
        1 -> words[0].take(1).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}

/**
 * One of the tints, always the same one for the same name. In dark the subtle fills sit too close
 * to the surface to tell apart, so the avatar mixes a mid tone of the same hue instead.
 */
private fun avatarTint(name: String, colors: AppColors): Color {
    val hues = listOf(
        colors.accentSubtle to colors.accent,
        colors.secondarySubtle to colors.secondary,
        colors.tertiarySubtle to colors.tertiary,
        colors.success.subtle to colors.success.base,
        colors.warning.subtle to colors.warning.base,
        colors.info.subtle to colors.info.base,
    )
    val (subtle, base) = hues[name.hashCode().absoluteValue % hues.size]
    return if (colors.isLight) subtle else lerp(colors.surface, base, DarkTintFraction)
}

private const val OverlapStep = 0.7f
private const val InitialsFraction = 0.4f
private const val DarkTintFraction = 0.42f

@AppComponentPreviews
@Composable
private fun AppAvatarPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppAvatar(name = "Ada Lovelace")
            AppAvatar(name = "grace")
            AppAvatar(name = "Maximiliana Kowalczyk-Brzęczyszczykiewicz", size = AppTheme.sizes.buttonLarge)
        }
        AppAvatarGroup(names = listOf("Ada Lovelace", "Grace Hopper", "Alan Turing"))
        AppAvatarGroup(names = listOf("Ada", "Grace", "Alan", "Edsger", "Barbara", "Donald"), maxShown = 3)
    }
}
