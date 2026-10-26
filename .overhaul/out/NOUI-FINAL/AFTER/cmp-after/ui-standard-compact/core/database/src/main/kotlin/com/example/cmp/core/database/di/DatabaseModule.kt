package com.example.cmp.core.database.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.cmp.core.common.coroutines.AppDispatchers
import com.example.cmp.core.common.session.SessionScopedStore
import com.example.cmp.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Builds each database once, on the platform's SQLite. Each data module takes its DAO from
 * [AppDatabase] in its own Hilt module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        dispatchers: AppDispatchers,
    ): AppDatabase = Room.databaseBuilder<AppDatabase>(context, AppDatabase.NAME)
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(dispatchers.io)
        .build()

    /** Everything in it belongs to the person signed in, so all of it goes when they sign out. */
    @Provides
    @IntoSet
    fun provideAppDatabaseClearing(database: AppDatabase): SessionScopedStore =
        SessionScopedStore { database.clearAllTables() }
}
