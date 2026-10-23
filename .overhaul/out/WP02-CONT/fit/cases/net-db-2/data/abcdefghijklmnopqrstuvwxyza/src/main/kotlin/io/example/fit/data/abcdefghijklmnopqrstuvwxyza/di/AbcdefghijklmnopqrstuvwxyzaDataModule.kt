package io.example.fit.data.abcdefghijklmnopqrstuvwxyza.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyza.AbcdefghijklmnopqrstuvwxyzaDao
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.OfflineFirstAbcdefghijklmnopqrstuvwxyzaRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.api.AbcdefghijklmnopqrstuvwxyzaRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzaDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzaRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwxyzaRepository,
    ): AbcdefghijklmnopqrstuvwxyzaRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwxyzaDao(
            database: AppDatabase,
        ): AbcdefghijklmnopqrstuvwxyzaDao = database.abcdefghijklmnopqrstuvwxyza()
    }
}
