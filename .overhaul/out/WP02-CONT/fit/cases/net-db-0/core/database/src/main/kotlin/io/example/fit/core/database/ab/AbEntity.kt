package io.example.fit.core.database.ab

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One ab row as last fetched.
 * `data/ab` maps it to its model.
 */
@Entity(tableName = "ab")
data class AbEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
