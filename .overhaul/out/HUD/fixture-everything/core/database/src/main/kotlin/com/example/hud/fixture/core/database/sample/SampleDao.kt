package com.example.hud.fixture.core.database.sample

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Reads are flows that follow every write; writes suspend. */
@Dao
interface SampleDao {

    @Query("SELECT * FROM samples ORDER BY position")
    fun observeAll(): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples WHERE id = :id")
    suspend fun byId(id: Int): SampleEntity?

    @Upsert
    suspend fun upsertAll(rows: List<SampleEntity>)

    @Query("DELETE FROM samples WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<Int>)

    /**
     * Makes the table the server's list in one transaction, so an observer never sees it half
     * written: rows the server no longer has go, the rest are written over in place.
     */
    @Transaction
    suspend fun replaceAll(rows: List<SampleEntity>) {
        deleteAllExcept(rows.map { it.id })
        upsertAll(rows)
    }
}
