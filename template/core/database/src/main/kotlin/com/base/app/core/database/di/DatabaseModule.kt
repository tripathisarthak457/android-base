package com.base.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.base.app.core.database.applyMigrations
import com.base.app.core.database.AppDatabase
import com.base.app.core.database.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Builds the database once, with every migration registered.
 *
 * Note what is absent: `fallbackToDestructiveMigration`. Leaving it out means a missing migration
 * is a crash on the developer's next run rather than silent data loss on a user's upgrade, which
 * is the trade this project wants — the crash happens to whoever can fix it.
 *
 * The DAO is provided separately so a repository depends on the table it uses rather than on the
 * whole database, which also stops a test having to build one to fake a single query.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .applyMigrations()
            .build()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.notes()
}
