package com.base.app.data.search.recent

/** The list logic, apart from storage, so it can be tested without a DataStore. */
internal object RecentSearches {
    const val LIMIT = 8
    private const val SEPARATOR = '\u001F'

    fun add(current: List<String>, query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return current
        return (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(LIMIT)
    }

    fun remove(current: List<String>, query: String): List<String> =
        current.filterNot { it.equals(query.trim(), ignoreCase = true) }

    // A unit separator rather than JSON: the values are single lines a person typed, and this
    // cannot fail to parse.
    fun encode(values: List<String>): String = values.joinToString(SEPARATOR.toString())

    fun decode(raw: String?): List<String> = raw?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
}
