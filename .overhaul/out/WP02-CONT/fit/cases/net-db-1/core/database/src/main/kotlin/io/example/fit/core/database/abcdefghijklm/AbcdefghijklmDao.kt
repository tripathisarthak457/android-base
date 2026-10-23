package io.example.fit.core.database.abcdefghijklm

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbcdefghijklmDao {

    @Query("SELECT * FROM abcdefghijklm ORDER BY position")
    fun observeAll(): Flow<List<AbcdefghijklmEntity>>

    @Query("SELECT * FROM abcdefghijklm WHERE id = :id")
    suspend fun byId(id: String): AbcdefghijklmEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbcdefghijklmEntity>)

    @Query("DELETE FROM abcdefghijklm WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbcdefghijklmEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
