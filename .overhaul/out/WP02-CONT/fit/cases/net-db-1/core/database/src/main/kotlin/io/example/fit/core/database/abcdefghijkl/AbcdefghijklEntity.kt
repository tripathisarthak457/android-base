package io.example.fit.core.database.abcdefghijkl

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijkl row as last fetched.
 * `data/abcdefghijkl` maps it to its model.
 */
@Entity(tableName = "abcdefghijkl")
data class AbcdefghijklEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
