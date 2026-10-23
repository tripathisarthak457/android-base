package io.example.fit.core.database.abcdefghijklmnopqrstu

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstu row as last fetched.
 * `data/abcdefghijklmnopqrstu` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstu")
data class AbcdefghijklmnopqrstuEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
