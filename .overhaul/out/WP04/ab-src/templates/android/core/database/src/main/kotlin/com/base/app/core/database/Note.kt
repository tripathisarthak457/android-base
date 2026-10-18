package com.base.app.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The reference entity. Delete it once you have one of your own; the shape is the part to keep. */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
)

/** Reads return [Flow], writes suspend. */
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
