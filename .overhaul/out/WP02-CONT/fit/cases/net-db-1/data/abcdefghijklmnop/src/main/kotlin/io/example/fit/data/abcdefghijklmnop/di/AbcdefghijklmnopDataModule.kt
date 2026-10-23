package io.example.fit.data.abcdefghijklmnop.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnop.AbcdefghijklmnopDao
import io.example.fit.data.abcdefghijklmnop.OfflineFirstAbcdefghijklmnopRepository
import io.example.fit.data.abcdefghijklmnop.api.AbcdefghijklmnopRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopDataModule {

    @Binds
    fun bindAbcdefghijklmnopRepository(
        impl: OfflineFirstAbcdefghijklmnopRepository,
    ): AbcdefghijklmnopRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopDao(database: AppDatabase): AbcdefghijklmnopDao =
            database.abcdefghijklmnop()
    }
}
