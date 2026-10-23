package io.example.fit.core.database.abcdefghijklmnopqrstuvwxyzabc

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvwxyzabc row as last fetched.
 * `data/abcdefghijklmnopqrstuvwxyzabc` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvwxyzabc")
data class AbcdefghijklmnopqrstuvwxyzabcEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
