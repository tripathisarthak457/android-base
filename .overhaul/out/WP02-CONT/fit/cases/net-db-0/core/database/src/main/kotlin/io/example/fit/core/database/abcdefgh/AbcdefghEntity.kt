package io.example.fit.core.database.abcdefgh

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefgh row as last fetched.
 * `data/abcdefgh` maps it to its model.
 */
@Entity(tableName = "abcdefgh")
data class AbcdefghEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
