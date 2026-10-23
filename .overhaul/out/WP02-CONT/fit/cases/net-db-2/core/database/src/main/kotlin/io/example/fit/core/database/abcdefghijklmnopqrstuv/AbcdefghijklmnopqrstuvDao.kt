package io.example.fit.core.database.abcdefghijklmnopqrstuv

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbcdefghijklmnopqrstuvDao {

    @Query("SELECT * FROM abcdefghijklmnopqrstuv ORDER BY position")
    fun observeAll(): Flow<List<AbcdefghijklmnopqrstuvEntity>>

    @Query("SELECT * FROM abcdefghijklmnopqrstuv WHERE id = :id")
    suspend fun byId(id: String): AbcdefghijklmnopqrstuvEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbcdefghijklmnopqrstuvEntity>)

    @Query("DELETE FROM abcdefghijklmnopqrstuv WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbcdefghijklmnopqrstuvEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
