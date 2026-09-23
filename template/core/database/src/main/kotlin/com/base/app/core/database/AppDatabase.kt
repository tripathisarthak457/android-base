package com.base.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** The app's data, and the only place a schema version is declared. */
@Database(
    entities = [NoteEntity::class],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notes(): NoteDao

    companion object {
        const val NAME = "app.db"
        const val VERSION = 2
    }
}

/**
 * Adds `notes.updatedAt`. That call is in most first drafts and it means "delete the user's data on
 * upgrade".
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

/** Every migration this database knows, in order. Registered in one place, by [applyMigrations]. */
val ALL_MIGRATIONS: List<Migration> = listOf(MIGRATION_1_2)

/** Registers every migration on a builder. */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyMigrations(): RoomDatabase.Builder<T> =
    also { builder -> ALL_MIGRATIONS.forEach { builder.addMigrations(it) } }
