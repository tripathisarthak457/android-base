package com.base.app.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.base.app.core.database.sample.SampleDao
import com.base.app.core.database.sample.SampleEntity

/**
 * The app's data, one table per domain, and the only place a schema version is declared. It
 * starts at version 1 and stays there until a release has shipped. After that, a schema change
 * bumps [VERSION] and registers a migration on the builder in `databaseModule`, tested against the
 * previous version's schema in `schemas/`; without one, opening the database fails on every
 * installed copy.
 */
@Database(
    entities = [SampleEntity::class],
    version = AppDatabase.VERSION,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun samples(): SampleDao

    companion object {
        const val NAME = "app.db"
        const val VERSION = 1
    }
}

// Kotlin/Native has no reflection, so Room generates each platform's actual of this constructor.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
