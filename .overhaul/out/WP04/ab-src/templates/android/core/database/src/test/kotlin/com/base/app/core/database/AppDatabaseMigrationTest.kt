package com.base.app.core.database

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The migration, replayed against a database that really was at version 1. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [MIGRATION_TEST_SDK])
class AppDatabaseMigrationTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        context.deleteDatabase(NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(NAME)
    }

    @Test
    fun `migrating from 1 to 2 keeps every row and defaults the new column`() {
        createVersionOne()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, NAME)
            .applyMigrations()
            .build()

        val notes = runBlocking { database.notes().all().first() }
        database.close()

        assertEquals(2, notes.size)
        assertEquals(setOf("Shopping", "Ideas"), notes.map { it.title }.toSet())
        // The rows predate the column, so every one of them takes the default rather than null —
        // which is the whole reason the ALTER has a DEFAULT on it.
        assertEquals(listOf(0L, 0L), notes.map { it.updatedAt })
    }

    /** The `notes` table as it was before `updatedAt`, with two rows already in it. */
    private fun createVersionOne() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`body` TEXT NOT NULL)",
                )
                db.execSQL("INSERT INTO notes (title, body) VALUES ('Shopping', 'milk')")
                db.execSQL("INSERT INTO notes (title, body) VALUES ('Ideas', 'a better one')")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(NAME)
                .callback(callback)
                .build(),
        )
        // Opening it is what runs onCreate; closing it leaves a version-1 file on disk for Room
        // to find and migrate.
        helper.writableDatabase.close()
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds
// with. The level only decides which SQLite the migration runs against.
private const val MIGRATION_TEST_SDK = 35
