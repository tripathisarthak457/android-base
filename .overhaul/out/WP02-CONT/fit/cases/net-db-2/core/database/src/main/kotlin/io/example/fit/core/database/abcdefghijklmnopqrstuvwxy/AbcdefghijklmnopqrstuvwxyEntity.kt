package io.example.fit.core.database.abcdefghijklmnopqrstuvwxy

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvwxy row as last fetched.
 * `data/abcdefghijklmnopqrstuvwxy` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvwxy")
data class AbcdefghijklmnopqrstuvwxyEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
