package io.example.fit.data.abcdefgh.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefgh.AbcdefghDao
import io.example.fit.data.abcdefgh.OfflineFirstAbcdefghRepository
import io.example.fit.data.abcdefgh.api.AbcdefghRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghDataModule {

    @Binds
    fun bindAbcdefghRepository(impl: OfflineFirstAbcdefghRepository): AbcdefghRepository

    companion object {
        @Provides
        fun provideAbcdefghDao(database: AppDatabase): AbcdefghDao = database.abcdefgh()
    }
}
