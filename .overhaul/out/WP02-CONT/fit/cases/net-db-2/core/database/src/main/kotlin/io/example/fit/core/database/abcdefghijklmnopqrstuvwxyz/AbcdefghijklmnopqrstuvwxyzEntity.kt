package io.example.fit.core.database.abcdefghijklmnopqrstuvwxyz

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvwxyz row as last fetched.
 * `data/abcdefghijklmnopqrstuvwxyz` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvwxyz")
data class AbcdefghijklmnopqrstuvwxyzEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
