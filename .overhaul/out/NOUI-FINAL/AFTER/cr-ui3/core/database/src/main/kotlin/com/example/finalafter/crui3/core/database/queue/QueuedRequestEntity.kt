package com.example.finalafter.crui3.core.database.queue

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A request that got no answer, kept to be sent again. A refused one stays too, with the server's
 * answer, because whoever made it has to be able to find out it was never accepted.
 */
@Entity(tableName = "queued_requests")
data class QueuedRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Who was signed in when it was made; it is only ever sent with their session. Null for a public call. */
    val owner: String?,
    val method: String,
    /** The path with its query already encoded onto it. */
    val path: String,
    val headersJson: String,
    val body: String?,
    val createdAtEpochMillis: Long,
    /** Passes that met no answer, or a refusal about the moment rather than the request. */
    val attempts: Int = 0,
    val refusedAtEpochMillis: Long? = null,
    /** The HTTP status of the refusal; null when the entry was given up on as too old. */
    val refusedStatus: Int? = null,
    val refusedCode: String? = null,
    val refusedMessage: String? = null,
)
