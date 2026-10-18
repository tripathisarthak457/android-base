package com.example.wp04.dlplay.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.wp04.dlplay.core.database.applyMigrations
import com.example.wp04.dlplay.core.database.AppDatabase
import com.example.wp04.dlplay.core.database.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Builds the database once, with every migration registered. */
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
