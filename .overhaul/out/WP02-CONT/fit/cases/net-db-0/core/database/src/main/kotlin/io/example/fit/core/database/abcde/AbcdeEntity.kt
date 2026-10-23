package io.example.fit.core.database.abcde

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcde row as last fetched.
 * `data/abcde` maps it to its model.
 */
@Entity(tableName = "abcde")
data class AbcdeEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
