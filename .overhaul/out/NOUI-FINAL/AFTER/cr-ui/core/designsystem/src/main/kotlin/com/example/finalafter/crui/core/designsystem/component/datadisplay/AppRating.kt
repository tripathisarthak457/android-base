package com.example.finalafter.crui.core.designsystem.component.datadisplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui.core.designsystem.R
import com.example.finalafter.crui.core.designsystem.component.text.AppIcon
import com.example.finalafter.crui.core.designsystem.foundation.interaction.PressFeedback
import com.example.finalafter.crui.core.designsystem.foundation.interaction.pressable
import com.example.finalafter.crui.core.designsystem.icon.AppIcons
import com.example.finalafter.crui.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui.core.designsystem.theme.AppTheme

/** A star rating, with real half stars. */
@Composable
fun AppRating(
    rating: Float,
    modifier: Modifier = Modifier,
    max: Int = 5,
    starSize: Dp = AppRatingDefaults.StarSize,
    onRatingChange: ((Int) -> Unit)? = null,
) {
    val colors = AppTheme.colors
    // "3.5 out of 5" rather than 3.5 on its own, which a screen reader would read as a bare number.
    val description = stringResource(R.string.designsystem_rating, rating, max)

    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(AppRatingDefaults.StarSpacing),
    ) {
        repeat(max) { index ->
            val fill = (rating - index).coerceIn(0f, 1f)
            val tapModifier = if (onRatingChange != null) {
                Modifier.pressable(onClick = { onRatingChange(index + 1) }, feedback = PressFeedback.Scale)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .then(tapModifier),
            ) {
                AppIcon(
                    imageVector = if (fill >= FullStar) AppIcons.StarFilled else AppIcons.Star,
                    contentDescription = null,
                    tint = if (fill >= FullStar) colors.warning.content else colors.outline,
                    size = starSize,
                )

                if (fill >= HalfStar && fill < FullStar) {
                    AppIcon(
                        imageVector = AppIcons.StarFilled,
                        contentDescription = null,
                        tint = colors.warning.content,
                        size = starSize,
                        modifier = Modifier.drawWithContent {
                            clipRect(right = size.width / 2f) { this@drawWithContent.drawContent() }
                        },
                    )
                }
            }
        }
    }
}

/** Above this much of a star is filled, it reads as whole; above half, as a half. */
private const val FullStar = 0.75f
private const val HalfStar = 0.25f

/** The rating's stars. */
object AppRatingDefaults {
    val StarSize = 18.dp
    val StarSpacing = 2.dp
}

@AppComponentPreviews
@Composable
private fun AppRatingPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppRating(rating = 0f)
        AppRating(rating = 3.5f)
        AppRating(rating = 4f, onRatingChange = {})
        AppRating(rating = 7f, max = 10, starSize = AppTheme.sizes.iconSmall)
    }
}
