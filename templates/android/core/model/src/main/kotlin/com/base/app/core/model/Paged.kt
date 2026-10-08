package com.base.app.core.model

/** One page of a list, and enough context to ask for the next one. */
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

    /** Appends the next page to what is already on screen. */
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
