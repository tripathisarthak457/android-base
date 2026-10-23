package io.example.fitcnetdb.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import io.example.fitcnetdb.core.database.abc.AbcDao
import io.example.fitcnetdb.core.database.abc.AbcEntity
import io.example.fitcnetdb.core.database.abcdefghi.AbcdefghiDao
import io.example.fitcnetdb.core.database.abcdefghi.AbcdefghiEntity
import io.example.fitcnetdb.core.database.abcdefghijklmno.AbcdefghijklmnoDao
import io.example.fitcnetdb.core.database.abcdefghijklmno.AbcdefghijklmnoEntity
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrs.AbcdefghijklmnopqrsDao
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrs.AbcdefghijklmnopqrsEntity
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvw.AbcdefghijklmnopqrstuvwDao
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvw.AbcdefghijklmnopqrstuvwEntity
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvwxyzabcd.AbcdefghijklmnopqrstuvwxyzabcdDao
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvwxyzabcd.AbcdefghijklmnopqrstuvwxyzabcdEntity

/**
 * The signed-in person's data, one table per domain, and the only place a schema version is
 * declared. It starts at version 1 and stays there until a release has shipped. After that, a schema
 * change bumps [VERSION] and registers a migration on the builder in `DatabaseModule`, tested
 * against the previous version's schema in `schemas/`; without one, opening the database fails on
 * every installed copy.
 */
@Database(
    entities = [
        AbcEntity::class,
        AbcdefghiEntity::class,
        AbcdefghijklmnoEntity::class,
        AbcdefghijklmnopqrsEntity::class,
        AbcdefghijklmnopqrstuvwEntity::class,
        AbcdefghijklmnopqrstuvwxyzabcdEntity::class,
    ],
    version = AppDatabase.VERSION,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun abc(): AbcDao
    abstract fun abcdefghi(): AbcdefghiDao
    abstract fun abcdefghijklmno(): AbcdefghijklmnoDao
    abstract fun abcdefghijklmnopqrs(): AbcdefghijklmnopqrsDao
    abstract fun abcdefghijklmnopqrstuvw(): AbcdefghijklmnopqrstuvwDao
    abstract fun abcdefghijklmnopqrstuvwxyzabcd(): AbcdefghijklmnopqrstuvwxyzabcdDao

    companion object {
        const val NAME = "app.db"
        const val VERSION = 1
    }
}
