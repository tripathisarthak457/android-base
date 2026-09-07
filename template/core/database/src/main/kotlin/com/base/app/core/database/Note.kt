package com.base.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The reference entity. Delete it once you have one of your own; the shape is the part to keep.
 *
 * `updatedAt` arrived in version 2 — see [MIGRATION_1_2] — so it is here to demonstrate the one
 * thing every schema eventually needs and most projects get wrong the first time.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
)

/**
 * Reads return [Flow], writes suspend.
 *
 * A flow rather than a one-shot read because a screen showing rows that another part of the app
 * can change has to be told; polling or re-querying on resume is how a list ends up disagreeing
 * with the database that produced it. Room emits again on every write to the tables the query
 * touches, so nothing has to remember to invalidate anything.
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC, id DESC")
    fun all(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
