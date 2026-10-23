package io.example.fit.core.database.abcdefghijklmnopqrstuvwx

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvwx row as last fetched.
 * `data/abcdefghijklmnopqrstuvwx` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvwx")
data class AbcdefghijklmnopqrstuvwxEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
