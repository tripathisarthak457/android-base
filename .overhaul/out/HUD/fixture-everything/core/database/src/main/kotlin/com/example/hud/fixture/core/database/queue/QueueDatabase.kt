package com.example.hud.fixture.core.database.queue

import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * Requests waiting to be sent, in a file of their own. [com.example.hud.fixture.core.database.AppDatabase] is
 * emptied on sign-out; this is not, because an involuntary sign-out must not lose what someone wrote
 * offline. Each entry names its owner instead, and is sent only with that person's session.
 */
@Database(entities = [QueuedRequestEntity::class], version = 1)
abstract class QueueDatabase : RoomDatabase() {

    abstract fun queuedRequests(): QueuedRequestDao

    companion object {
        const val NAME = "queue.db"
    }
}
