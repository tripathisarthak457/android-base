package io.example.fit.core.database.abcdefghij

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghij row as last fetched.
 * `data/abcdefghij` maps it to its model.
 */
@Entity(tableName = "abcdefghij")
data class AbcdefghijEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
