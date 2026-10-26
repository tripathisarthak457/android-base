package com.example.cmp.core.database.queue

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QueuedRequestDao {

    @Insert
    suspend fun insert(entity: QueuedRequestEntity): Long

    /** What may be sent now: not refused, and made by [owner] or by nobody, oldest first. */
    @Query(
        "SELECT * FROM queued_requests WHERE refusedAtEpochMillis IS NULL " +
            "AND (owner IS NULL OR owner = :owner) ORDER BY id",
    )
    suspend fun sendable(owner: String?): List<QueuedRequestEntity>

    /** Everything [owner] made, refused entries included, for a screen that lists what is unsent. */
    @Query("SELECT * FROM queued_requests WHERE owner IS NULL OR owner = :owner ORDER BY id")
    fun observe(owner: String?): Flow<List<QueuedRequestEntity>>

    @Query("UPDATE queued_requests SET attempts = attempts + 1 WHERE id = :id")
    suspend fun countAttempt(id: Long)

    @Query(
        "UPDATE queued_requests SET refusedAtEpochMillis = :at, refusedStatus = :status, " +
            "refusedCode = :code, refusedMessage = :message WHERE id = :id",
    )
    suspend fun markRefused(id: Long, at: Long, status: Int?, code: String?, message: String?)

    /** Puts a refused entry back in line, after whoever made it has fixed what the server objected to. */
    @Query(
        "UPDATE queued_requests SET refusedAtEpochMillis = NULL, refusedStatus = NULL, refusedCode = NULL, " +
            "refusedMessage = NULL, attempts = 0 WHERE id = :id",
    )
    suspend fun requeue(id: Long)

    @Query("DELETE FROM queued_requests WHERE id = :id")
    suspend fun delete(id: Long)
}
