package com.base.app.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.animation.AppAppear
import com.base.app.core.designsystem.animation.AppStaggeredColumn
import com.base.app.core.designsystem.animation.busyOverlay
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.selection.AppSwitch
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * The motion vocabulary, running.
 *
 * Every transition here comes from `rememberAppTransitions()`, which means two things worth
 * seeing rather than reading: they all use the theme's durations and easings, so nothing on a
 * screen is half a beat out of step with anything else; and they all collapse to no animation
 * when the device has "remove animations" turned on. Turn it on in Developer options and come
 * back to this page — every example still works, none of them move.
 */
@Composable
fun MotionSection() {
    val transitions = rememberAppTransitions()

    var fade by remember { mutableStateOf(true) }
    var expand by remember { mutableStateOf(true) }
    var rise by remember { mutableStateOf(true) }
    var pop by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    var staggerKey by remember { mutableIntStateOf(0) }
    var appearKey by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    if (busy) {
        LaunchedEffect(busy) {
            delay(BUSY_MILLIS)
            busy = false
        }
    }

    CatalogGroup(
        title = "Fade",
        caption = "The default. For content that appears where it already is.",
    ) {
        MotionToggle(label = "Visible", checked = fade, onCheckedChange = { fade = it })
        AnimatedVisibility(visible = fade, enter = transitions.fadeIn, exit = transitions.fadeOut) {
            DemoCard("Fades in and out")
        }
    }

    CatalogGroup(
        title = "Expand",
        caption = "For a block that opens in place. The fade finishes before the box does.",
    ) {
        MotionToggle(label = "Open", checked = expand, onCheckedChange = { expand = it })
        AnimatedVisibility(
            visible = expand,
            enter = transitions.expandIn,
            exit = transitions.collapseOut,
        ) {
            DemoCard("Grows from nothing")
        }
    }

    CatalogGroup(title = "Rise", caption = "For something arriving from the bottom edge.") {
        MotionToggle(label = "Shown", checked = rise, onCheckedChange = { rise = it })
        AnimatedVisibility(visible = rise, enter = transitions.riseIn, exit = transitions.sinkOut) {
            DemoCard("Slides up from below")
        }
    }

    CatalogGroup(title = "Pop", caption = "For an element that should read as appearing, not moving.") {
        MotionToggle(label = "Shown", checked = pop, onCheckedChange = { pop = it })
        AnimatedVisibility(visible = pop, enter = transitions.popIn, exit = transitions.popOut) {
            DemoCard("Scales up into place")
        }
    }

    CatalogGroup(
        title = "Slide",
        caption = "A lateral move. Going back moves the other way, which is what makes it a line.",
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                transitions.slideIn(forward) togetherWith transitions.slideOut(forward)
            },
            label = "motionSlide",
        ) { current ->
            DemoCard("Step ${current + 1} of $SLIDE_STEPS")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppButton(
                text = "Back",
                onClick = {
                    forward = false
                    step = (step - 1 + SLIDE_STEPS) % SLIDE_STEPS
                },
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
            )
            AppButton(
                text = "Next",
                onClick = {
                    forward = true
                    step = (step + 1) % SLIDE_STEPS
                },
                size = ButtonSize.Small,
            )
        }
    }

    CatalogGroup(
        title = "Staggered column",
        caption = "For a fixed set of rows arriving together. Capped at eight, then all at once.",
    ) {
        // `key` throws the whole subtree away and rebuilds it, which is what re-runs the
        // entrance. Toggling a flag inside would only animate the rows that changed.
        key(staggerKey) {
            AppStaggeredColumn(itemCount = STAGGER_ROWS) { index ->
                AppListItem(
                    title = "Row ${index + 1}",
                    supporting = "Delayed by ${index * STAGGER_STEP_MILLIS} ms",
                )
            }
        }
        AppButton(
            text = "Replay",
            onClick = { staggerKey++ },
            variant = ButtonVariant.Ghost,
            size = ButtonSize.Small,
        )
    }

    CatalogGroup(
        title = "Appear",
        caption = "One-shot entrance for content that arrives after a load.",
    ) {
        key(appearKey) {
            AppAppear { DemoCard("Lifted into place on first composition") }
        }
        AppButton(
            text = "Replay",
            onClick = { appearKey++ },
            variant = ButtonVariant.Ghost,
            size = ButtonSize.Small,
        )
    }

    CatalogGroup(
        title = "Busy overlay",
        caption = "Dims and blocks input without hiding what the user just typed.",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .busyOverlay(busy),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppListItem(title = "Delivery address", supporting = "12 Residency Road, Bengaluru")
            AppListItem(title = "Payment", supporting = "Visa ending 4242")
        }
        AppButton(
            text = if (busy) "Saving…" else "Save",
            onClick = { busy = true },
            enabled = !busy,
            fillWidth = true,
        )
    }
}

@Composable
private fun MotionToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
        AppText(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.contentSecondary,
        )
    }
}

@Composable
private fun DemoCard(text: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = AppTheme.spacing.sm),
            contentAlignment = Alignment.CenterStart,
        ) {
            AppText(
                text = text,
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.contentPrimary,
            )
        }
    }
}

private const val SLIDE_STEPS = 3
private const val STAGGER_ROWS = 6
private const val STAGGER_STEP_MILLIS = 28
private const val BUSY_MILLIS = 1_600L
