package io.example.audit.core.designsystem.component.sheet.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import io.example.audit.core.designsystem.component.sheet.AppSheetDefaults
import io.example.audit.core.designsystem.component.sheet.AppSheetScope
import io.example.audit.core.designsystem.component.sheet.AppSheetStyle
import io.example.audit.core.designsystem.component.sheet.OutsideSheetScope
import io.example.audit.core.designsystem.component.sheet.SheetController
import io.example.audit.core.designsystem.component.sheet.SheetDismissSource
import io.example.audit.core.designsystem.foundation.layout.LocalEntryVisibility

/**
 * What a sheet's content asked of it: a header or footer in place of the call site's, a look, a guard
 * on closing, a step back to take before closing. A registration from an entry that is uncovered wins
 * over one from a covered entry; among the rest, the last registered, which is the innermost, wins.
 *
 * A registration made before the sheet first moves holds its arrival until the slot host has composed
 * and measured it, so a sheet that fits its content rises to a height that includes it.
 */
@Stable
internal class SheetSlots(private val controller: SheetController) {
    private val headers = mutableStateListOf<SlotEntry>()
    private val footers = mutableStateListOf<SlotEntry>()
    private val styles = mutableStateListOf<StyleEntry>()
    private val guards = mutableStateListOf<Guard>()
    private val backs = mutableStateListOf<StepBack>()

    /** A provider registered and its host has not composed it yet. */
    var pending by mutableStateOf(false)
        private set

    /** The title of an `AppSheetHeader(String)` in the call site's header: the sheet's pane title. */
    var headerTitle: String? by mutableStateOf(null)

    val header: SlotEntry? get() = pick(headers)
    val footer: SlotEntry? get() = pick(footers)
    val style: StyleEntry? get() = styles.lastOrNull { it.uncovered() } ?: styles.lastOrNull()
    val guard: Guard? get() = guards.lastOrNull { it.uncovered() }
    val stepBack: StepBack? get() = backs.lastOrNull { it.uncovered() }

    private fun pick(entries: List<SlotEntry>): SlotEntry? = entries.lastOrNull { it.uncovered() } ?: entries.lastOrNull()

    fun add(entry: SlotEntry, footer: Boolean) {
        if (!controller.sheetState.isVisible || controller.sheetState.presenting) pending = true
        if (footer) footers += entry else headers += entry
    }

    fun remove(entry: SlotEntry) {
        headers -= entry
        footers -= entry
    }

    fun add(entry: StyleEntry) {
        if (!controller.sheetState.isVisible) pending = true
        styles += entry
    }

    fun remove(entry: StyleEntry) {
        styles -= entry
    }

    fun add(guard: Guard) {
        guards += guard
    }

    fun remove(guard: Guard) {
        guards -= guard
    }

    fun add(step: StepBack) {
        backs += step
    }

    fun remove(step: StepBack) {
        backs -= step
    }

    /** A slot host composed what was registered: the sheet may rise now. */
    fun markComposed() {
        if (pending) pending = false
    }
}

/**
 * Header or footer content provided from inside a sheet. It is composed in the sheet's slot with the
 * locals and the saved state of the composition that provided it, so a destination's footer gets its
 * screen's ViewModels, lifecycle and theme, never the host's.
 */
@Stable
internal class SlotEntry(
    content: @Composable AppSheetScope.() -> Unit,
    title: String?,
    private val locals: CompositionLocalContext,
    private val holder: SaveableStateHolder,
    private val key: Any,
    val uncovered: () -> Boolean,
) {
    var content: @Composable AppSheetScope.() -> Unit by mutableStateOf(content)
    var title: String? by mutableStateOf(title)

    @Composable
    fun Compose(scope: AppSheetScope) {
        CompositionLocalProvider(locals) {
            holder.SaveableStateProvider(key) { content(scope) }
        }
    }
}

@Stable
internal class StyleEntry(
    style: AppSheetStyle,
    grabber: (@Composable AppSheetScope.() -> Unit)?,
    val uncovered: () -> Boolean,
) {
    var style by mutableStateOf(style)
    var grabber by mutableStateOf(grabber)
}

internal class Guard(val onAttempt: (SheetDismissSource) -> Unit, val uncovered: () -> Boolean)

internal class StepBack(val onBack: () -> Unit, val uncovered: () -> Boolean)

internal val LocalSheetSlots = staticCompositionLocalOf<SheetSlots?> { null }

/**
 * Puts [content] in the enclosing sheet's header slot, replacing `header`. [title] names the sheet
 * for screen readers. Outside a sheet it draws in place.
 */
@Composable
fun ProvideSheetHeader(title: String? = null, content: @Composable AppSheetScope.() -> Unit) =
    ProvideSlot(footer = false, title = title, content = content)

/** Puts [content] in the enclosing sheet's footer slot, pinned to its visible bottom. In place outside a sheet. */
@Composable
fun ProvideSheetFooter(content: @Composable AppSheetScope.() -> Unit) = ProvideSlot(footer = true, title = null, content = content)

@Composable
private fun ProvideSlot(footer: Boolean, title: String?, content: @Composable AppSheetScope.() -> Unit) {
    val slots = LocalSheetSlots.current
    if (slots == null) {
        OutsideSheetScope.content()
        return
    }
    val locals = currentCompositionLocalContext
    val holder = rememberSaveableStateHolder()
    val key = currentCompositeKeyHashCode
    val visibility = LocalEntryVisibility.current
    val entry = remember(slots) { SlotEntry(content, title, locals, holder, key) { visibility.isUncovered } }
    SideEffect {
        entry.content = content
        entry.title = title
    }
    DisposableEffect(slots, entry) {
        slots.add(entry, footer)
        onDispose { slots.remove(entry) }
    }
}

/**
 * Sets the enclosing sheet's look and grabber from its content: for a destination sheet, whose
 * AppSheet call the screen never sees. Lands before the first visible frame. Nothing outside a sheet.
 */
@Composable
fun ProvideSheetStyle(
    style: AppSheetStyle,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
) {
    val slots = LocalSheetSlots.current ?: return
    val visibility = LocalEntryVisibility.current
    val entry = remember(slots) { StyleEntry(style, grabber) { visibility.isUncovered } }
    SideEffect {
        entry.style = style
        entry.grabber = grabber
    }
    DisposableEffect(slots, entry) {
        slots.add(entry)
        onDispose { slots.remove(entry) }
    }
}
