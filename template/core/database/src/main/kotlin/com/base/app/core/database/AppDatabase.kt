package com.base.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app's data, and the only place a schema version is declared.
 *
 * `exportSchema` is on and the JSON under `schemas/` is committed. That directory is not an
 * artefact — it is what a migration test reads to open the database as it was at an older version,
 * and it is what makes reviewing a schema change possible at all: the diff shows the table, not
 * just the Kotlin that happens to generate it.
 */
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
 * Adds `notes.updatedAt`.
 *
 * ## Why this is written by hand rather than left to `fallbackToDestructiveMigration`
 *
 * That call is in most first drafts and it means "delete the user's data on upgrade". It is
 * invisible in development, where the data is fake and a reinstall is free, and it is a support
 * incident the first time it reaches somebody who had six months of notes in the app.
 *
 * ## Why the column has a default
 *
 * `ALTER TABLE ... ADD COLUMN` cannot add a NOT NULL column without one — SQLite has to have an
 * answer for every row that already exists. Room compares the migrated database against the
 * schema it generated and fails loudly if the two disagree, which is why the default here and the
 * `@ColumnInfo(defaultValue = "0")` on the entity have to say the same thing.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SupportSQLiteDatabase) {
        connection.execSQL("ALTER TABLE notes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

/** Every migration this database knows, in order. Registered in one place, by [applyMigrations]. */
val ALL_MIGRATIONS: List<Migration> = listOf(MIGRATION_1_2)

/**
 * Registers every migration on a builder.
 *
 * A loop rather than a spread: the list is the source of truth, and both the app and the
 * migration test have to register exactly the same set — a test that registered fewer would
 * pass against a database the app could not open.
 */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyMigrations(): RoomDatabase.Builder<T> =
    also { builder -> ALL_MIGRATIONS.forEach { builder.addMigrations(it) } }
