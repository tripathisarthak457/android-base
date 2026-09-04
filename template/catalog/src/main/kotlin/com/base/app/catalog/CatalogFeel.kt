package com.base.app.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.selection.AppChip
import com.base.app.core.designsystem.component.selection.AppSwitch
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.HapticEffect
import com.base.app.core.designsystem.foundation.LocalAppHapticsEnabled
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppMotionStyle
import com.base.app.core.designsystem.theme.AppTheme

/**
 * How the app feels under a finger, which is the part a screenshot cannot show.
 *
 * Three things happen on every tap and they are meant to be felt rather than noticed: the state
 * layer, the shrink, and the overshoot on release. A fourth — the haptic — is the one people
 * describe as "expensive" without being able to say why.
 *
 * Both are one setting each at `AppTheme`, so this page is a way of choosing them rather than a
 * gallery of things somebody has to implement.
 */
@Composable
fun FeelSection() {
    var style by remember { mutableStateOf(AppMotionStyle.Standard) }
    val hapticsDefault = LocalAppHapticsEnabled.current
    var haptics by remember(hapticsDefault) { mutableStateOf(hapticsDefault) }

    CatalogGroup(
        title = "Motion style",
        caption = "AppTheme(motionStyle = …). Pick one, then press the controls below.",
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppMotionStyle.entries.forEach { option ->
                AppChip(
                    label = option.name,
                    selected = style == option,
                    onClick = { style = option },
                )
            }
        }
        AppText(
            text = style.describe(),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentTertiary,
        )
    }

    CatalogGroup(
        title = "Haptics",
        caption = "AppTheme(hapticsEnabled = …). The device's own setting still applies on top.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSwitch(checked = haptics, onCheckedChange = { haptics = it })
            AppText(
                text = if (haptics) "On" else "Off",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentSecondary,
            )
        }
    }

    // A nested theme, so everything below responds with the style chosen above while the chips
    // that choose it keep the app's own. Two AppThemes in one tree is exactly what this is for.
    AppTheme(motionStyle = style, hapticsEnabled = haptics) {
        CatalogGroup(
            title = "Try it",
            caption = "Press and hold, then release. The pop on release is the part that matters.",
        ) {
            AppButton("Primary", {}, fillWidth = true)
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                AppButton("Small", {}, size = ButtonSize.Small)
                AppButton("Secondary", {}, variant = ButtonVariant.Secondary)
                AppButton("Ghost", {}, variant = ButtonVariant.Ghost)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                AppIconButton(AppIcons.Heart, "Favourite", {})
                AppIconButton(AppIcons.Share, "Share", {}, variant = ButtonVariant.Secondary)
                AppIconButton(AppIcons.Trash, "Delete", {}, variant = ButtonVariant.Destructive)
            }
            AppCard(onClick = {}) {
                AppText(
                    text = "A whole card is a control too",
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.contentPrimary,
                )
            }
        }

        CatalogGroup(
            title = "The vocabulary",
            caption = "Each effect names what just happened, not a waveform.",
        ) {
            val player = rememberAppHaptics()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                HapticEffect.entries.forEach { effect ->
                    AppButton(
                        text = effect.name,
                        onClick = { player.perform(effect) },
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Small,
                    )
                }
            }
            AppText(
                text = "Older devices fall back to the nearest constant they have, so the " +
                    "distinctions get coarser rather than disappearing.",
                style = AppTheme.typography.caption,
                color = AppTheme.colors.contentTertiary,
            )
        }
    }
}

private fun AppMotionStyle.describe(): String = when (this) {
    AppMotionStyle.Bouncy ->
        "Springy, with a visible overshoot. Playful; wrong for anything that handles money."
    AppMotionStyle.Standard -> "Crisp, a trace of overshoot. Lively without asking for attention."
    AppMotionStyle.Calm -> "No overshoot, slightly longer. For dense professional interfaces."
    AppMotionStyle.Snappy -> "The shortest duration that still reads as motion. For utilities."
}
