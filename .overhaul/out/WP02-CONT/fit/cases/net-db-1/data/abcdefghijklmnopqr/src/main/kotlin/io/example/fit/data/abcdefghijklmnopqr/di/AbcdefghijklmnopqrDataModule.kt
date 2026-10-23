package io.example.fit.data.abcdefghijklmnopqr.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqr.AbcdefghijklmnopqrDao
import io.example.fit.data.abcdefghijklmnopqr.OfflineFirstAbcdefghijklmnopqrRepository
import io.example.fit.data.abcdefghijklmnopqr.api.AbcdefghijklmnopqrRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrRepository(
        impl: OfflineFirstAbcdefghijklmnopqrRepository,
    ): AbcdefghijklmnopqrRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrDao(database: AppDatabase): AbcdefghijklmnopqrDao =
            database.abcdefghijklmnopqr()
    }
}
