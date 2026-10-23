package io.example.fit.core.database.abcdefghijklmn

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmn row as last fetched.
 * `data/abcdefghijklmn` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmn")
data class AbcdefghijklmnEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
