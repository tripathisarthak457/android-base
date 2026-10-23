package io.example.fit.core.database.ab

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbDao {

    @Query("SELECT * FROM ab ORDER BY position")
    fun observeAll(): Flow<List<AbEntity>>

    @Query("SELECT * FROM ab WHERE id = :id")
    suspend fun byId(id: String): AbEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbEntity>)

    @Query("DELETE FROM ab WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
