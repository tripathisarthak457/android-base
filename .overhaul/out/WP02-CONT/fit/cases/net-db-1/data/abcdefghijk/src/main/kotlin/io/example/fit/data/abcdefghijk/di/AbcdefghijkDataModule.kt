package io.example.fit.data.abcdefghijk.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijk.AbcdefghijkDao
import io.example.fit.data.abcdefghijk.OfflineFirstAbcdefghijkRepository
import io.example.fit.data.abcdefghijk.api.AbcdefghijkRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijkDataModule {

    @Binds
    fun bindAbcdefghijkRepository(impl: OfflineFirstAbcdefghijkRepository): AbcdefghijkRepository

    companion object {
        @Provides
        fun provideAbcdefghijkDao(database: AppDatabase): AbcdefghijkDao = database.abcdefghijk()
    }
}
