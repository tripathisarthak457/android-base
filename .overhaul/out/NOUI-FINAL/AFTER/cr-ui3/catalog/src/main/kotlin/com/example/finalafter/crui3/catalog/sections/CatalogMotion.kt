package com.example.finalafter.crui3.catalog.sections

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui3.catalog.CatalogGroup
import com.example.finalafter.crui3.core.designsystem.animation.AppAppear
import com.example.finalafter.crui3.core.designsystem.animation.AppStaggeredColumn
import com.example.finalafter.crui3.core.designsystem.animation.AppSymbolReplace
import com.example.finalafter.crui3.core.designsystem.animation.appAnimateItem
import com.example.finalafter.crui3.core.designsystem.animation.busyOverlay
import com.example.finalafter.crui3.core.designsystem.animation.popWhenOn
import com.example.finalafter.crui3.core.designsystem.animation.rememberAppTransitions
import com.example.finalafter.crui3.core.designsystem.animation.rememberCountUp
import com.example.finalafter.crui3.core.designsystem.animation.symbolBounce
import com.example.finalafter.crui3.core.designsystem.component.button.AppButton
import com.example.finalafter.crui3.core.designsystem.component.button.ButtonSize
import com.example.finalafter.crui3.core.designsystem.component.button.ButtonVariant
import com.example.finalafter.crui3.core.designsystem.component.list.AppRow
import com.example.finalafter.crui3.core.designsystem.component.text.AppFiguresText
import com.example.finalafter.crui3.core.designsystem.component.text.AppIcon
import com.example.finalafter.crui3.core.designsystem.component.text.AppText
import com.example.finalafter.crui3.core.designsystem.foundation.layout.bleed
import com.example.finalafter.crui3.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui3.core.designsystem.icon.AppIcons
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

@Composable
internal fun MotionSection() {
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
        LazyColumn(Modifier.fillMaxWidth().height(240.dp), flingBehavior = AppScrollDefaults.flingBehavior()) {
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
    CatalogGroup(
        "Deceleration",
        caption = "Every list flings as an iOS scroll view does, keeping 0.998 of its speed each millisecond. " +
            "Flick this one: the readout sets the fling against iOS's curve for the same release.",
    ) {
        DecelerationLab()
    }
    CatalogGroup("Edges", caption = "Every list rubber-bands at its ends, as on iOS. This one asks for Android's stretch instead.") {
        CompositionLocalProvider(LocalOverscrollFactory provides rememberPlatformOverscrollFactory()) {
            LazyColumn(Modifier.fillMaxWidth().height(200.dp), flingBehavior = AppScrollDefaults.flingBehavior()) {
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

@Composable
private fun DecelerationLab() {
    var last by remember { mutableStateOf<FlingRun?>(null) }
    val base = AppScrollDefaults.flingBehavior()
    val fling = remember(base) { MeasuredFling(base) { last = it } }
    val density = LocalDensity.current.density
    // The group's own spacing, so the list and its readout sit as they would as its children.
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        LazyColumn(Modifier.fillMaxWidth().height(320.dp).testTag("fling_lab"), flingBehavior = fling) {
            items(DecelerationRows, key = { it }) { AppRow(title = "Row ${it + 1}") }
        }
        AppFiguresText(
            text = last?.describe(density) ?: "Flick the list.",
            modifier = Modifier.testTag("fling_readout"),
            style = AppTheme.typography.footnote,
        )
    }
}

/** One fling: its release speed in pixels a second, the pixels it travelled and how long it took. */
private class FlingRun(val velocity: Float, val distance: Float, val millis: Long) {
    fun describe(density: Float): String {
        // UIScrollView.DecelerationRate.normal as a continuous rate, a second.
        val rate = 1000.0 * ln(1.0 / 0.998)
        val iosDistance = velocity / rate
        val iosSeconds = ln(velocity / RestVelocity) / rate
        val took = "%.2f".format(Locale.ROOT, millis / 1000.0)
        val iosTook = "%.2f".format(Locale.ROOT, iosSeconds)
        return "Released at ${(velocity / density).roundToInt()} dp/s: ${(distance / density).roundToInt()} dp in " +
            "$took s. iOS: ${(iosDistance / density).roundToInt()} dp in $iosTook s."
    }
}

/** [base], reporting what each fling did once it ends. */
private class MeasuredFling(private val base: FlingBehavior, private val onFlung: (FlingRun) -> Unit) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val outer = this
        var travelled = 0f
        val counting = object : ScrollScope {
            override fun scrollBy(pixels: Float): Float = outer.scrollBy(pixels).also { travelled += it }
        }
        val start = SystemClock.uptimeMillis()
        val left = with(base) { counting.performFling(initialVelocity) }
        if (abs(initialVelocity) > RestVelocity) onFlung(FlingRun(abs(initialVelocity), abs(travelled), SystemClock.uptimeMillis() - start))
        return left
    }
}

private const val DecelerationRows = 400

/** The design system's fling ends here, in pixels a second; a release slower than this is no fling. */
private const val RestVelocity = 30f
