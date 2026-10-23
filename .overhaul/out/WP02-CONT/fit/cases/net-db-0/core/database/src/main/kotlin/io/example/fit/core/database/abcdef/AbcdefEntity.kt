package io.example.fit.core.database.abcdef

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdef row as last fetched.
 * `data/abcdef` maps it to its model.
 */
@Entity(tableName = "abcdef")
data class AbcdefEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
