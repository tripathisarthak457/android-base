package io.example.fit.core.database.abcdefghijklmnop

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnop row as last fetched.
 * `data/abcdefghijklmnop` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnop")
data class AbcdefghijklmnopEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
