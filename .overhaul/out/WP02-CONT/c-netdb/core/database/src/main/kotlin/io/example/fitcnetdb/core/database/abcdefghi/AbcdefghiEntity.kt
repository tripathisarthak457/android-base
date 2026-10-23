package io.example.fitcnetdb.core.database.abcdefghi

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghi row as last fetched.
 * `data/abcdefghi` maps it to its model.
 */
@Entity(tableName = "abcdefghi")
data class AbcdefghiEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
