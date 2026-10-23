package io.example.fit.core.database.abcdefghijklmnopqrstuvwxyza

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One abcdefghijklmnopqrstuvwxyza row as last fetched.
 * `data/abcdefghijklmnopqrstuvwxyza` maps it to its model.
 */
@Entity(tableName = "abcdefghijklmnopqrstuvwxyza")
data class AbcdefghijklmnopqrstuvwxyzaEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
