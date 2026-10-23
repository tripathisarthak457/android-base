package io.example.fit.data.abcdefghijklmnopq.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopq.AbcdefghijklmnopqDao
import io.example.fit.data.abcdefghijklmnopq.OfflineFirstAbcdefghijklmnopqRepository
import io.example.fit.data.abcdefghijklmnopq.api.AbcdefghijklmnopqRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqDataModule {

    @Binds
    fun bindAbcdefghijklmnopqRepository(
        impl: OfflineFirstAbcdefghijklmnopqRepository,
    ): AbcdefghijklmnopqRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqDao(database: AppDatabase): AbcdefghijklmnopqDao =
            database.abcdefghijklmnopq()
    }
}
