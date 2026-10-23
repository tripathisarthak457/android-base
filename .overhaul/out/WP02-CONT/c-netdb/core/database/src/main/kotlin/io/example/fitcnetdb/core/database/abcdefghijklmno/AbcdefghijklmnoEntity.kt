package io.example.fitcnetdb.core.database.abcdefghijklmno

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmno row as last fetched.
 * `data/abcdefghijklmno` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmno")
data class AbcdefghijklmnoEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
