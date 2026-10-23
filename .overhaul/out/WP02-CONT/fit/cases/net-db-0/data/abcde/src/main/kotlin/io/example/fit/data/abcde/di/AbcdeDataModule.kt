package io.example.fit.data.abcde.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcde.AbcdeDao
import io.example.fit.data.abcde.OfflineFirstAbcdeRepository
import io.example.fit.data.abcde.api.AbcdeRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdeDataModule {

    @Binds
    fun bindAbcdeRepository(impl: OfflineFirstAbcdeRepository): AbcdeRepository

    companion object {
        @Provides
        fun provideAbcdeDao(database: AppDatabase): AbcdeDao = database.abcde()
    }
}
