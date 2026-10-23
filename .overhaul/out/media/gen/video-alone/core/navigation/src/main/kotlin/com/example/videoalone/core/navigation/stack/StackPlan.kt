package com.example.videoalone.core.navigation.stack

import com.example.videoalone.core.navigation.NavPane

/** Where an entry is laid out: the whole host, or one pane of a page that shows several. */
internal enum class Slot { Full, List, Detail, Extra, Main, Supporting }

/** The detail pane of a list page with no detail open yet. */
internal data class EmptyDetail(val list: Any)

/**
 * One screen's worth of a stack. A page is one entry, or, when the window has room for more than one
 * pane, a list with the details and extras opened from it, or a main screen with its supporting
 * panes, shown side by side. Pages slide over each other; inside a page panes swap in place.
 *
 * @property members the content keys of every entry in the page, oldest first.
 * @property shown what the page draws and where: one entry at full width, or a pane per [Slot].
 */
internal class StackPage(val members: List<Any>, val shown: Map<Any, Slot>) {
    val single: Boolean get() = shown.size == 1
}

/** How a stack change looks: the page on top slides in over the one it covers, or off it, or nothing moves. */
internal enum class Slide { None, Push, Pop }

/**
 * A stack as it is drawn after a change, and how the change gets there.
 *
 * @property upper what slides: the arriving page on a push, the leaving one on a pop, and the page
 *   on top otherwise.
 * @property lower what [upper] slides over: the page it covers, or the one a pop returns to.
 */
internal class StackPlan(
    val keys: List<Any>,
    val pages: List<StackPage>,
    val slide: Slide,
    val upper: Map<Any, Slot>,
    val lower: Map<Any, Slot>,
) {
    val top: StackPage get() = pages.last()

    val beneath: StackPage? get() = pages.getOrNull(pages.lastIndex - 1)

    /** The panes [key] is shown with: the page it slides in, or the page that shows it. */
    private fun pageOf(key: Any): Map<Any, Slot> =
        when (key) {
            in upper -> upper
            in lower -> lower
            else -> pages.firstOrNull { key in it.shown }?.shown.orEmpty()
        }

    /** Where [key] is laid out: in the page that shows it, or full width while parked. */
    fun slotOf(key: Any): Slot = pageOf(key)[key] ?: Slot.Full

    /**
     * The left edge and width of [key] in a host [width] px wide, before mirroring for a
     * right-to-left layout. A list, an extra and a supporting pane are [pane] px wide; a detail or
     * a main screen takes what they leave.
     */
    fun boundsOf(key: Any, width: Int, pane: Int): PaneBounds {
        val page = pageOf(key)
        val side = pane.coerceAtMost(width / 2)
        val start = if (Slot.List in page.values) side else 0
        val end = if (Slot.Extra in page.values || Slot.Supporting in page.values) side else 0
        return when (page[key] ?: Slot.Full) {
            Slot.Full -> PaneBounds(0, width)
            Slot.List -> PaneBounds(0, side)
            Slot.Extra, Slot.Supporting -> PaneBounds(width - side, side)
            Slot.Detail, Slot.Main -> PaneBounds(start, width - start - end)
        }
    }

    /** Whether another pane of [key]'s page sits on its leading side, so a divider separates them. */
    fun followsAnotherPane(key: Any): Boolean {
        val page = pageOf(key)
        return when (page[key]) {
            Slot.Detail -> Slot.List in page.values
            Slot.Extra, Slot.Supporting -> true
            else -> false
        }
    }

    /** Detail panes with nothing open, in any page that draws one. */
    val emptyDetails: List<EmptyDetail> =
        (pages.flatMap { it.shown.keys } + upper.keys + lower.keys).filterIsInstance<EmptyDetail>().distinct()
}

/** Where a pane sits in its host: [left] and [width] in px. */
internal data class PaneBounds(val left: Int, val width: Int)

/**
 * Splits a stack into pages, oldest first, as ranges of stack indices. With room for two [columns]
 * or more, a list and the details and extras stacked on it share one page, and so do a main screen
 * and the supporting panes over it; any other entry is a page of its own.
 */
internal fun stackPages(panes: List<NavPane>, columns: Int): List<IntRange> {
    val pages = ArrayList<IntRange>(panes.size)
    var start = 0
    while (start < panes.size) {
        var end = start
        if (columns >= 2) {
            while (end + 1 < panes.size && joins(panes[start], panes.subList(start + 1, end + 1), panes[end + 1])) end++
        }
        pages += start..end
        start = end + 1
    }
    return pages
}

/** Whether [next] joins the page [first] starts, which already holds [joined]: an extra needs a detail first. */
private fun joins(first: NavPane, joined: List<NavPane>, next: NavPane): Boolean = when (first) {
    NavPane.List -> next == NavPane.Detail || (next == NavPane.Extra && NavPane.Detail in joined)
    NavPane.Main -> next == NavPane.Supporting
    else -> false
}

internal fun pagesOf(keys: List<Any>, panes: List<NavPane>, columns: Int): List<StackPage> =
    stackPages(panes, columns).map { range ->
        val members = keys.subList(range.first, range.last + 1).toList()
        val kinds = panes.subList(range.first, range.last + 1)
        StackPage(members, shownPanes(members, kinds, columns))
    }

/**
 * What a page of [columns] draws. A list page shows the list, its newest detail (or an empty detail
 * pane) and, from three columns, the newest extra opened after that detail; with two columns an
 * open extra takes the list's place, as the detail moves over to make room for it. A main page shows
 * the main screen and its newest supporting pane.
 */
private fun shownPanes(members: List<Any>, kinds: List<NavPane>, columns: Int): Map<Any, Slot> {
    val first = members.first()
    // With one column a list is a screen like any other: an empty detail pane beside it would take
    // half a phone's width.
    if (columns < 2 || (members.size == 1 && kinds.first() != NavPane.List)) return mapOf(first to Slot.Full)
    return when (kinds.first()) {
        NavPane.List -> {
            val lastDetail = kinds.lastIndexOf(NavPane.Detail)
            val lastExtra = kinds.lastIndexOf(NavPane.Extra).takeIf { it > lastDetail }
            val detail: Any = if (lastDetail >= 0) members[lastDetail] else EmptyDetail(first)
            when {
                lastExtra == null -> mapOf(first to Slot.List, detail to Slot.Detail)
                columns >= 3 -> mapOf(first to Slot.List, detail to Slot.Detail, members[lastExtra] to Slot.Extra)
                else -> mapOf(detail to Slot.Detail, members[lastExtra] to Slot.Extra)
            }
        }
        NavPane.Main -> mapOf(first to Slot.Main, members.last() to Slot.Supporting)
        else -> mapOf(first to Slot.Full)
    }
}

/**
 * What the change from [previous] to [keys] looks like. A pop is a change whose new top page was
 * already under the old one; anything else that changes the top page is a push, including a
 * replace. The same keys in other pages (a fold, an unfold, a resize across a pane width) and a
 * change inside the top page are not navigation the eye should follow, so nothing slides.
 *
 * [slides] says whether the destination that would move asks for a slide at all.
 */
internal fun planChange(
    previous: StackPlan?,
    keys: List<Any>,
    panes: List<NavPane>,
    columns: Int,
    slides: (Any) -> Boolean,
): StackPlan {
    val pages = pagesOf(keys, panes, columns)
    val top = pages.last()
    val beneath = pages.getOrNull(pages.lastIndex - 1)
    val atRest = StackPlan(keys, pages, Slide.None, top.shown, beneath?.shown.orEmpty())
    if (previous == null || previous.keys == keys) return atRest
    val oldTop = previous.top
    if (oldTop.members.first() == top.members.first()) return atRest
    return if (top.members.first() in previous.keys) {
        if (!slides(oldTop.members.last())) atRest else StackPlan(keys, pages, Slide.Pop, oldTop.shown, top.shown)
    } else {
        if (!slides(top.members.last())) atRest else StackPlan(keys, pages, Slide.Push, top.shown, oldTop.shown)
    }
}
