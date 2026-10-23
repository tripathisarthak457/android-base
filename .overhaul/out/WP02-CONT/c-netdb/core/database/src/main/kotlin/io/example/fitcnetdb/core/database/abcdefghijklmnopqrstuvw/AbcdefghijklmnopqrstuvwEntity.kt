package io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvw

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvw row as last fetched.
 * `data/abcdefghijklmnopqrstuvw` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvw")
data class AbcdefghijklmnopqrstuvwEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
