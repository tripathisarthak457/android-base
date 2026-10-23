package io.example.fit.data.abcdefghijklmnopqrst.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrst.AbcdefghijklmnopqrstDao
import io.example.fit.data.abcdefghijklmnopqrst.OfflineFirstAbcdefghijklmnopqrstRepository
import io.example.fit.data.abcdefghijklmnopqrst.api.AbcdefghijklmnopqrstRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstRepository,
    ): AbcdefghijklmnopqrstRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstDao(database: AppDatabase): AbcdefghijklmnopqrstDao =
            database.abcdefghijklmnopqrst()
    }
}
