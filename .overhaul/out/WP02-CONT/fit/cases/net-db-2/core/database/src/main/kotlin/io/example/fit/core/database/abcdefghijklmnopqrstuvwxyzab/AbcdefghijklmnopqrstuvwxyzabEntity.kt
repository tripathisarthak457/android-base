package io.example.fit.core.database.abcdefghijklmnopqrstuvwxyzab

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvwxyzab row as last fetched.
 * `data/abcdefghijklmnopqrstuvwxyzab` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvwxyzab")
data class AbcdefghijklmnopqrstuvwxyzabEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
