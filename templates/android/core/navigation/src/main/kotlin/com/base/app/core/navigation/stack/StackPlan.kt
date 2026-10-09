package com.base.app.core.navigation.stack

import com.base.app.core.navigation.NavPane

/** Where an entry is laid out: the whole host, or one side of a list and its detail. */
internal enum class Slot { Full, List, Detail }

/** The detail pane of a list page with no detail open yet. */
internal data class EmptyDetail(val list: Any)

/**
 * One screen's worth of a stack. A page is one entry, or, when the window has room for two panes,
 * a list together with the details opened from it, shown side by side. Pages slide over each other;
 * inside a page the detail pane swaps in place.
 *
 * @property members the content keys of every entry in the page, oldest first.
 * @property shown what the page draws and where: one entry at full width, or the list and its newest
 *   detail (or an [EmptyDetail]).
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

    /** Where [key] is laid out: in the page that shows it, or full width while parked. */
    fun slotOf(key: Any): Slot = upper[key] ?: lower[key] ?: pages.firstNotNullOfOrNull { it.shown[key] } ?: Slot.Full

    /** Detail panes with nothing open, in any page that draws one. */
    val emptyDetails: List<EmptyDetail> =
        (pages.flatMap { it.shown.keys } + upper.keys + lower.keys).filterIsInstance<EmptyDetail>().distinct()
}

/**
 * Splits a stack into pages, oldest first, as ranges of stack indices. With [twoPanes], a list and
 * the details stacked on it share one page; a detail with no list under it is a page of its own.
 */
internal fun stackPages(panes: List<NavPane>, twoPanes: Boolean): List<IntRange> {
    val pages = ArrayList<IntRange>(panes.size)
    var start = 0
    while (start < panes.size) {
        var end = start
        if (twoPanes && panes[start] == NavPane.List) {
            while (end + 1 < panes.size && panes[end + 1] == NavPane.Detail) end++
        }
        pages += start..end
        start = end + 1
    }
    return pages
}

internal fun pagesOf(keys: List<Any>, panes: List<NavPane>, twoPanes: Boolean): List<StackPage> =
    stackPages(panes, twoPanes).map { range ->
        val members = keys.subList(range.first, range.last + 1)
        val shown = if (twoPanes && panes[range.first] == NavPane.List) {
            val list = members.first()
            val detail = if (members.size > 1) members.last() else EmptyDetail(list)
            mapOf(list to Slot.List, detail to Slot.Detail)
        } else {
            mapOf(members.single() to Slot.Full)
        }
        StackPage(members.toList(), shown)
    }

/**
 * What the change from [previous] to [keys] looks like. A pop is a change whose new top page was
 * already under the old one; anything else that changes the top page is a push, including a
 * replace. The same keys in other pages (a fold, an unfold, a resize across the two-pane width) and
 * a change inside the top page are not navigation the eye should follow, so nothing slides.
 *
 * [slides] says whether the destination that would move asks for a slide at all.
 */
internal fun planChange(
    previous: StackPlan?,
    keys: List<Any>,
    panes: List<NavPane>,
    twoPanes: Boolean,
    slides: (Any) -> Boolean,
): StackPlan {
    val pages = pagesOf(keys, panes, twoPanes)
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
