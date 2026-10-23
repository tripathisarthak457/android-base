package io.example.fit.core.database.abcdefghijklmnopqrstuv

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuv row as last fetched.
 * `data/abcdefghijklmnopqrstuv` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuv")
data class AbcdefghijklmnopqrstuvEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
