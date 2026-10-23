package io.example.fit.core.database.abcdefg

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefg row as last fetched.
 * `data/abcdefg` maps it to its model.
 */
@Entity(tableName = "abcdefg")
data class AbcdefgEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
