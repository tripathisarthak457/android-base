package com.base.app.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
// <opt:sample>
import com.base.app.core.database.sample.SampleDao
import com.base.app.core.database.sample.SampleEntity
// </opt:sample>
// <generated:database-imports>

/**
 * The signed-in person's data, one table per domain, and the only place a schema version is
 * declared. It starts at version 1 and stays there until a release has shipped. After that, a schema
 * change bumps [VERSION] and registers a migration on the builder in `DatabaseModule`, tested
 * against the previous version's schema in `schemas/`; without one, opening the database fails on
 * every installed copy.
 */
@Database(
    entities = [
        // <opt:sample>
        SampleEntity::class,
        // </opt:sample>
        // <generated:database-entities>
    ],
    version = AppDatabase.VERSION,
)
abstract class AppDatabase : RoomDatabase() {
    // <opt:sample>

    abstract fun samples(): SampleDao
    // </opt:sample>
    // <generated:database-daos>

    companion object {
        const val NAME = "app.db"
        const val VERSION = 1
    }
}
