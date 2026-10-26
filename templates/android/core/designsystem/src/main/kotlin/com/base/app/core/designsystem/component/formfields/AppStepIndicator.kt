package com.base.app.core.designsystem.component.formfields

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Where someone is in a flow of a few steps — checkout, sign-up, a setup wizard. Finished steps
 * show a check, the current one is ringed, and the line between them fills as they go.
 *
 * Read by TalkBack as one sentence, "Step 2 of 4: Payment", rather than a row of numbers.
 */
@Composable
public fun AppStepIndicator(steps: List<String>, currentStep: Int, modifier: Modifier = Modifier) {
    val current = currentStep.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
    val description = steps.getOrNull(current)?.let {
        stringResource(R.string.designsystem_step_of, current + 1, steps.size, it)
    }.orEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        steps.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Connectors(
                        hasBefore = index > 0,
                        hasAfter = index < steps.lastIndex,
                        beforeDone = index <= current,
                        afterDone = index < current,
                    )
                    StepNode(number = index + 1, done = index < current, active = index == current)
                }
                AppText(
                    text = label,
                    modifier = Modifier.padding(horizontal = AppStepIndicatorDefaults.LabelInset),
                    style = AppTheme.typography.caption2,
                    color = if (index ==
                        current
                    ) {
                        AppTheme.colors.contentPrimary
                    } else {
                        AppTheme.colors.contentSecondary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Connectors(
    hasBefore: Boolean,
    hasAfter: Boolean,
    beforeDone: Boolean,
    afterDone: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ConnectorHalf(visible = hasBefore, done = beforeDone, modifier = Modifier.weight(1f))
        ConnectorHalf(visible = hasAfter, done = afterDone, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ConnectorHalf(visible: Boolean, done: Boolean, modifier: Modifier = Modifier) {
    val colour = animateColorAsState(
        targetValue = when {
            !visible -> Color.Transparent
            done -> AppTheme.colors.accent
            else -> AppTheme.colors.separator
        },
        animationSpec = AppTheme.springs.slowFade(),
        label = "stepConnector",
    )
    Box(
        modifier = modifier.height(AppStepIndicatorDefaults.ConnectorHeight).drawBehind {
            drawRect(colour.value)
        },
    )
}

@Composable
private fun StepNode(number: Int, done: Boolean, active: Boolean) {
    val colors = AppTheme.colors
    val fill = animateColorAsState(
        targetValue = if (done) colors.accent else colors.surface,
        animationSpec = AppTheme.springs.slowFade(),
        label = "stepFill",
    )
    val ring = animateColorAsState(
        targetValue = if (done || active) colors.accent else colors.outline,
        animationSpec = AppTheme.springs.slowFade(),
        label = "stepRing",
    )

    val ringWidth = if (active) AppStepIndicatorDefaults.ActiveRingWidth else 1.dp
    Box(
        modifier = Modifier
            .size(AppStepIndicatorDefaults.NodeSize)
            .drawBehind {
                val stroke = ringWidth.toPx()
                drawCircle(fill.value)
                drawCircle(
                    ring.value,
                    radius = (size.minDimension - stroke) / 2,
                    style = Stroke(stroke),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            AppIcon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = colors.onAccent,
                size = AppTheme.sizes.iconSmall,
            )
        } else {
            AppText(
                text = number.toString(),
                style = AppTheme.typography.caption2,
                color = if (active) colors.accentContent else colors.contentSecondary,
            )
        }
    }
}

/** The step indicator's nodes, labels and connectors. */
public object AppStepIndicatorDefaults {
    public val LabelInset: Dp = 2.dp
    public val ConnectorHeight: Dp = 2.dp
    public val ActiveRingWidth: Dp = 2.dp
    public val NodeSize: Dp = 26.dp
}

@AppComponentPreviews
@Composable
private fun AppStepIndicatorPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppStepIndicator(steps = listOf("Cart", "Address", "Payment"), currentStep = 0)
        AppStepIndicator(steps = listOf("Cart", "Address", "Payment"), currentStep = 1)
        AppStepIndicator(
            steps = listOf("Your details", "Delivery address", "Payment method", "Review"),
            currentStep = 3,
        )
    }
}
