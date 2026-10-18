package com.example.hud.fixture.core.database.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.hud.fixture.core.common.coroutines.AppDispatchers
import com.example.hud.fixture.core.common.session.SessionScopedStore
import com.example.hud.fixture.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import com.example.hud.fixture.core.database.queue.QueueDatabase
import com.example.hud.fixture.core.database.queue.QueuedRequestDao

/**
 * Builds each database once, on the platform's SQLite. Each data module takes its DAO from
 * [AppDatabase] in its own Hilt module.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context, dispatchers: AppDispatchers): AppDatabase =
        Room.databaseBuilder<AppDatabase>(context, AppDatabase.NAME)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(dispatchers.io)
            .build()

    /** Everything in it belongs to the person signed in, so all of it goes when they sign out. */
    @Provides
    @IntoSet
    fun provideAppDatabaseClearing(database: AppDatabase): SessionScopedStore =
        SessionScopedStore { database.clearAllTables() }

    @Provides
    @Singleton
    fun provideQueueDatabase(@ApplicationContext context: Context, dispatchers: AppDispatchers): QueueDatabase =
        Room.databaseBuilder<QueueDatabase>(context, QueueDatabase.NAME)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(dispatchers.io)
            .build()

    @Provides
    fun provideQueuedRequestDao(database: QueueDatabase): QueuedRequestDao = database.queuedRequests()
}
