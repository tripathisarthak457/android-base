package io.example.fitcnetdb.core.database.abcdefghijklmnopqrs

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrs row as last fetched.
 * `data/abcdefghijklmnopqrs` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrs")
data class AbcdefghijklmnopqrsEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
