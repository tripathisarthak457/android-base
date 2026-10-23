package io.example.fit.core.database.abcdefghijklmnopqr

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbcdefghijklmnopqrDao {

    @Query("SELECT * FROM abcdefghijklmnopqr ORDER BY position")
    fun observeAll(): Flow<List<AbcdefghijklmnopqrEntity>>

    @Query("SELECT * FROM abcdefghijklmnopqr WHERE id = :id")
    suspend fun byId(id: String): AbcdefghijklmnopqrEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbcdefghijklmnopqrEntity>)

    @Query("DELETE FROM abcdefghijklmnopqr WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbcdefghijklmnopqrEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
