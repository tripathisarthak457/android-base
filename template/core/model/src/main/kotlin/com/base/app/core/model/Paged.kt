package com.base.app.core.model

/**
 * One page of a list, and enough context to ask for the next one.
 *
 * Lives in `:core:model` rather than in a data module because paging is not a domain concept —
 * orders, products and notifications all page identically, and a per-domain copy of this is three
 * chances to get the "is there a next page" arithmetic subtly different.
 *
 * [hasNext] is derived rather than trusted from the wire. Backends disagree about whether the
 * last page reports a next cursor, and a screen that believes an incorrect flag either stops one
 * page early or requests forever.
 */
data class Paged<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
) {
    val hasNext: Boolean get() = page * pageSize < totalItems

    val isEmpty: Boolean get() = items.isEmpty() && page == FIRST_PAGE

    fun <R> map(transform: (T) -> R): Paged<R> = Paged(
        items = items.map(transform),
        page = page,
        pageSize = pageSize,
        totalItems = totalItems,
    )

    /**
     * Appends the next page to what is already on screen.
     *
     * Rejects anything that is not the immediate successor. A double-tapped "load more" fires the
     * same request twice, and appending its response twice duplicates every row — the guard is
     * cheaper than de-duplicating by id afterwards, and does not require the items to have one.
     */
    operator fun plus(next: Paged<T>): Paged<T> {
        if (next.page != page + 1) return this
        return next.copy(items = items + next.items)
    }

    companion object {
        const val FIRST_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 20

        fun <T> empty(pageSize: Int = DEFAULT_PAGE_SIZE): Paged<T> =
            Paged(emptyList(), FIRST_PAGE, pageSize, 0)
    }
}
