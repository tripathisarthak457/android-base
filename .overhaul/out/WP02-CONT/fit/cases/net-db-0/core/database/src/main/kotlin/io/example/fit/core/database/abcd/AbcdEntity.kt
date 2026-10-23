package io.example.fit.core.database.abcd

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcd row as last fetched.
 * `data/abcd` maps it to its model.
 */
@Entity(tableName = "abcd")
data class AbcdEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
