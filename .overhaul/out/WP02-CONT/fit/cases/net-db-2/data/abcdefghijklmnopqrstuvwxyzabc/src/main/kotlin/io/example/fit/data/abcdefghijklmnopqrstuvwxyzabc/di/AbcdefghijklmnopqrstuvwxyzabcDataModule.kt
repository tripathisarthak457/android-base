package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyzabc.AbcdefghijklmnopqrstuvwxyzabcDao
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.OfflineFirstAbcdefghijklmnopqrstuvwxyzabcRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.AbcdefghijklmnopqrstuvwxyzabcRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzabcDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzabcRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwxyzabcRepository,
    ): AbcdefghijklmnopqrstuvwxyzabcRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwxyzabcDao(
            database: AppDatabase,
        ): AbcdefghijklmnopqrstuvwxyzabcDao = database.abcdefghijklmnopqrstuvwxyzabc()
    }
}
