package io.example.fit.core.database.abcdefghij

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface AbcdefghijDao {

    @Query("SELECT * FROM abcdefghij ORDER BY position")
    fun observeAll(): Flow<List<AbcdefghijEntity>>

    @Query("SELECT * FROM abcdefghij WHERE id = :id")
    suspend fun byId(id: String): AbcdefghijEntity?

    @Upsert
    suspend fun upsertAll(rows: List<AbcdefghijEntity>)

    @Query("DELETE FROM abcdefghij WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<String>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<AbcdefghijEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
