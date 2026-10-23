package io.example.fit.core.database.abcdefghijklmnopq

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbcdefghijklmnopqDao {

    @Query("SELECT * FROM abcdefghijklmnopq ORDER BY position")
    fun observeAll(): Flow<List<AbcdefghijklmnopqEntity>>

    @Query("SELECT * FROM abcdefghijklmnopq WHERE id = :id")
    suspend fun byId(id: String): AbcdefghijklmnopqEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbcdefghijklmnopqEntity>)

    @Query("DELETE FROM abcdefghijklmnopq WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbcdefghijklmnopqEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
