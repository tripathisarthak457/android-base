package io.example.fit.data.abcdefghijklmnopqrstuvwx.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuvwx.AbcdefghijklmnopqrstuvwxDao
import io.example.fit.data.abcdefghijklmnopqrstuvwx.OfflineFirstAbcdefghijklmnopqrstuvwxRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.AbcdefghijklmnopqrstuvwxRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwxRepository,
    ): AbcdefghijklmnopqrstuvwxRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwxDao(database: AppDatabase): AbcdefghijklmnopqrstuvwxDao =
            database.abcdefghijklmnopqrstuvwx()
    }
}
