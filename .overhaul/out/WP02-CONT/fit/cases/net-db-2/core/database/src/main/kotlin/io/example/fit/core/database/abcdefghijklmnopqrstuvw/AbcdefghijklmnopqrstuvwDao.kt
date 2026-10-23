package io.example.fit.core.database.abcdefghijklmnopqrstuvw

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbcdefghijklmnopqrstuvwDao {

    @Query("SELECT * FROM abcdefghijklmnopqrstuvw ORDER BY position")
    fun observeAll(): Flow<List<AbcdefghijklmnopqrstuvwEntity>>

    @Query("SELECT * FROM abcdefghijklmnopqrstuvw WHERE id = :id")
    suspend fun byId(id: String): AbcdefghijklmnopqrstuvwEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbcdefghijklmnopqrstuvwEntity>)

    @Query("DELETE FROM abcdefghijklmnopqrstuvw WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbcdefghijklmnopqrstuvwEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
