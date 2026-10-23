package io.example.fit.core.database.note

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A starter table, because Room needs at least one. Rename it for the app's first domain,
 * or delete it once another table exists.
 */
@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Where the row sat in the list it came in, so the list reads back in that order. */
    val position: Int,
)
