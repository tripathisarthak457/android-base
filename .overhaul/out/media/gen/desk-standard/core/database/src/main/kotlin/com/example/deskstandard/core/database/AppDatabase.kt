package com.example.deskstandard.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.deskstandard.core.database.sample.SampleDao
import com.example.deskstandard.core.database.sample.SampleEntity

/**
 * The signed-in person's data, one table per domain, and the only place a schema version is
 * declared. It starts at version 1 and stays there until a release has shipped. After that, a schema
 * change bumps [VERSION] and registers a migration on the builder in `DatabaseModule`, tested
 * against the previous version's schema in `schemas/`; without one, opening the database fails on
 * every installed copy.
 */
@Database(
    entities = [
        SampleEntity::class,
    ],
    version = AppDatabase.VERSION,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun samples(): SampleDao

    companion object {
        const val NAME = "app.db"
        const val VERSION = 1
    }
}
