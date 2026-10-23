package io.example.fit.core.database.abcdefghijklmnopqr

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqr row as last fetched.
 * `data/abcdefghijklmnopqr` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqr")
data class AbcdefghijklmnopqrEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
