package io.example.fit.core.database.abcdefghijklmnopq

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopq row as last fetched.
 * `data/abcdefghijklmnopq` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopq")
data class AbcdefghijklmnopqEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
