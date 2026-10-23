package io.example.fit.core.database.abcdefghijklmnopqrst

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrst row as last fetched.
 * `data/abcdefghijklmnopqrst` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrst")
data class AbcdefghijklmnopqrstEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
