package com.example.everything.data.search.remote

import com.example.everything.data.search.api.model.SearchResult
import com.example.everything.data.search.remote.dto.SearchHitDto

internal fun SearchHitDto.toDomain(snippetLength: Int = SNIPPET_LENGTH) = SearchResult(
    id = id,
    title = title.trim().replaceFirstChar { it.uppercase() },
    snippet = body.replace('\n', ' ').trim().take(snippetLength),
)

private const val SNIPPET_LENGTH = 120
