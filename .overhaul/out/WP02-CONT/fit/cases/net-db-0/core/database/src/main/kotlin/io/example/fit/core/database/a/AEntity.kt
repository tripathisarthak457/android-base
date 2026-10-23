package io.example.fit.core.database.a

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One a row as last fetched.
 * `data/a` maps it to its model.
 */
@Entity(tableName = "a")
data class AEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
