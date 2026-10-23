package io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvwxyzabcd

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvwxyzabcd row as last fetched.
 * `data/abcdefghijklmnopqrstuvwxyzabcd` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvwxyzabcd")
data class AbcdefghijklmnopqrstuvwxyzabcdEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
