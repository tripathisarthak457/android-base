package io.example.fit.core.database.abcdefghijklmnopqrstuvwxyza

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbcdefghijklmnopqrstuvwxyzaDao {

    @Query("SELECT * FROM abcdefghijklmnopqrstuvwxyza ORDER BY position")
    fun observeAll(): Flow<List<AbcdefghijklmnopqrstuvwxyzaEntity>>

    @Query("SELECT * FROM abcdefghijklmnopqrstuvwxyza WHERE id = :id")
    suspend fun byId(id: String): AbcdefghijklmnopqrstuvwxyzaEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbcdefghijklmnopqrstuvwxyzaEntity>)

    @Query("DELETE FROM abcdefghijklmnopqrstuvwxyza WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbcdefghijklmnopqrstuvwxyzaEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
