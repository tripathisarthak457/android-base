package io.example.fit.core.database.abcdefghijklm

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklm row as last fetched.
 * `data/abcdefghijklm` maps it to its model.
 */
@Entity(tableName = "abcdefghijklm")
data class AbcdefghijklmEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
