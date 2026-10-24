package com.example.everything.catalog.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.everything.catalog.CatalogGroup
import com.example.everything.core.designsystem.component.button.AppButton
import com.example.everything.core.designsystem.component.button.AppIconButton
import com.example.everything.core.designsystem.component.button.AppLinkButton
import com.example.everything.core.designsystem.component.button.ButtonSize
import com.example.everything.core.designsystem.component.button.ButtonVariant
import com.example.everything.core.designsystem.icon.AppIcons
import com.example.everything.core.designsystem.theme.AppTheme

@Composable
fun ButtonsSection() {
    CatalogGroup("Variants", caption = "Filled for the one action a screen exists for; tinted for a real alternative; plain for the rest. Destructive is red on a red tint, readable in dark theme.") {
        ButtonVariant.entries.forEach { variant ->
            Wrap {
                AppButton(text = variant.name, onClick = {}, variant = variant)
                AppButton(text = "Disabled", onClick = {}, variant = variant, enabled = false)
            }
        }
    }
    CatalogGroup("Sizes and icons", caption = "Heights are minimums, so a label at a large font size still fits.") {
        ButtonSize.entries.forEach { size ->
            Wrap {
                AppButton(text = size.name, onClick = {}, size = size)
                AppButton(text = "Add", onClick = {}, size = size, variant = ButtonVariant.Tinted, leadingIcon = AppIcons.Plus)
                AppButton(text = "Next", onClick = {}, size = size, variant = ButtonVariant.Plain, trailingIcon = AppIcons.ChevronRight)
            }
        }
        AppButton(text = "Full width", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
    CatalogGroup("Loading", caption = "The label keeps its space, so the button does not change size.") {
        var loading by remember { mutableStateOf(true) }
        Wrap {
            AppButton(text = "Pay now", onClick = { loading = !loading }, loading = loading)
            AppButton(text = if (loading) "Stop" else "Start", onClick = { loading = !loading }, variant = ButtonVariant.Tinted)
        }
    }
    CatalogGroup("Icon buttons", caption = "A plain glyph dims; a tinted or filled circle shrinks. All answer across 48dp.") {
        ButtonSize.entries.forEach { size ->
            Wrap {
                AppIconButton(AppIcons.Share, contentDescription = "Share", onClick = {}, size = size)
                AppIconButton(AppIcons.Heart, contentDescription = "Like", onClick = {}, size = size, variant = ButtonVariant.Tinted)
                AppIconButton(AppIcons.Plus, contentDescription = "Add", onClick = {}, size = size, variant = ButtonVariant.Filled)
                AppIconButton(AppIcons.Trash, contentDescription = "Delete", onClick = {}, size = size, variant = ButtonVariant.Destructive)
                AppIconButton(AppIcons.Edit, contentDescription = "Edit", onClick = {}, size = size, enabled = false)
            }
        }
    }
    CatalogGroup("Links") {
        Wrap {
            AppLinkButton(text = "Forgot password?", onClick = {})
            AppLinkButton(text = "Disabled link", onClick = {}, enabled = false)
        }
    }
}

@Composable
internal fun Wrap(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) { content() }
}
