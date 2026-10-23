package io.example.fit.core.database.abc

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abc row as last fetched.
 * `data/abc` maps it to its model.
 */
@Entity(tableName = "abc")
data class AbcEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
