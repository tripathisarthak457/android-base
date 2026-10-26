package com.example.cmp.data.search.api.model

/** One match, with the text that matched. */
data class SearchResult(
    val id: Int,
    val title: String,
    val snippet: String,
)
