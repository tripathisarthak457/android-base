package com.base.app.catalog.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberPlatformOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.catalog.CatalogGroup
import com.base.app.core.designsystem.animation.AppAppear
import com.base.app.core.designsystem.animation.AppStaggeredColumn
import com.base.app.core.designsystem.animation.AppSymbolReplace
import com.base.app.core.designsystem.animation.appAnimateItem
import com.base.app.core.designsystem.animation.busyOverlay
import com.base.app.core.designsystem.animation.popWhenOn
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.animation.rememberCountUp
import com.base.app.core.designsystem.animation.symbolBounce
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.text.AppFiguresText
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.layout.bleed
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

@Composable
fun MotionSection() {
    CatalogGroup("Transitions", caption = "Anything that moves rides a spring, so it can be interrupted; only opacity fades.") {
        val transitions = rememberAppTransitions()
        var shown by remember { mutableStateOf(true) }
        AppButton(text = if (shown) "Hide" else "Show", onClick = { shown = !shown }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
        listOf(
            "fadeIn / fadeOut" to (transitions.fadeIn to transitions.fadeOut),
            "expandIn / collapseOut" to (transitions.expandIn to transitions.collapseOut),
            "riseIn / sinkOut" to (transitions.riseIn to transitions.sinkOut),
            "popIn / popOut" to (transitions.popIn to transitions.popOut),
            "slideIn / slideOut" to (transitions.slideIn() to transitions.slideOut()),
        ).forEach { (name, pair) ->
            Box(Modifier.fillMaxWidth().height(40.dp)) {
                AnimatedVisibility(visible = shown, enter = pair.first, exit = pair.second) {
                    Box(Modifier.fillMaxSize().background(AppTheme.colors.accentSubtle, AppTheme.shapes.sm)) {
                        AppText(text = name, style = AppTheme.typography.footnote, color = AppTheme.colors.accentContent)
                    }
                }
            }
        }
    }
    CatalogGroup("First appearance", caption = "AppAppear lifts one block in; AppStaggeredColumn brings items in one after another. Once per saved state.") {
        var round by remember { mutableIntStateOf(0) }
        AppButton(text = "Replay", onClick = { round++ }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
        key(round) {
            AppAppear { AppText(text = "AppAppear", style = AppTheme.typography.headline) }
            AppStaggeredColumn(itemCount = 5) { AppRow(title = "Staggered row ${it + 1}") }
        }
    }
    CatalogGroup("List placement", caption = "appAnimateItem moves rows on the layout spring when the list changes.") {
        var items by remember { mutableStateOf((1..5).toList()) }
        AppButton(text = "Shuffle", onClick = { items = items.shuffled() }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
        LazyColumn(Modifier.fillMaxWidth().height(240.dp)) {
            items(items, key = { it }) { item -> AppRow(title = "Row $item", modifier = appAnimateItem()) }
        }
    }
    CatalogGroup("Emphasis", caption = "popWhenOn overshoots once when a mark takes; rememberCountUp runs a number up.") {
        var liked by remember { mutableStateOf(false) }
        var count by remember { mutableIntStateOf(1_240) }
        AppIcon(
            if (liked) AppIcons.HeartFilled else AppIcons.Heart,
            contentDescription = "Like",
            tint = if (liked) AppTheme.colors.danger.content else AppTheme.colors.contentSecondary,
            size = 32.dp,
            modifier = Modifier.popWhenOn(liked),
        )
        AppButton(text = if (liked) "Unlike" else "Like", onClick = { liked = !liked }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
        AppFiguresText(text = rememberCountUp(count).toString(), style = AppTheme.typography.title2)
        AppButton(text = "Add 500", onClick = { count += 500 }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
    }
    CatalogGroup("Symbol effects", caption = "symbolBounce dips a glyph and springs it back when its value changes; AppSymbolReplace swaps one glyph for another.") {
        var alerts by remember { mutableIntStateOf(0) }
        var dark by remember { mutableStateOf(false) }
        AppIcon(AppIcons.Bell, contentDescription = "Notifications", size = 32.dp, modifier = Modifier.symbolBounce(alerts))
        AppButton(text = "Notify", onClick = { alerts++ }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
        AppSymbolReplace(icon = if (dark) AppIcons.Moon else AppIcons.Sun, contentDescription = if (dark) "Dark" else "Light", size = 32.dp)
        AppButton(text = if (dark) "Light" else "Dark", onClick = { dark = !dark }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
    }
    CatalogGroup("Edges", caption = "Every list rubber-bands at its ends, as on iOS. This one asks for Android's stretch instead.") {
        CompositionLocalProvider(LocalOverscrollFactory provides rememberPlatformOverscrollFactory()) {
            LazyColumn(Modifier.fillMaxWidth().height(200.dp)) {
                items((1..8).toList(), key = { it }) { AppRow(title = "Pull past me $it") }
            }
        }
    }
    CatalogGroup("Bleed and busy", caption = "bleed runs content past a padded column's gutter; busyOverlay dims a form and takes its taps while it submits.") {
        Box(Modifier.bleed(AppTheme.spacing.gutter).fillMaxWidth().height(40.dp).background(AppTheme.colors.fill))
        var busy by remember { mutableStateOf(true) }
        Column(Modifier.busyOverlay(busy)) {
            AppButton(text = "Submit", onClick = {})
        }
        AppButton(text = if (busy) "Done" else "Busy", onClick = { busy = !busy }, variant = ButtonVariant.Plain, size = ButtonSize.Small)
    }
}
