package io.example.fit.data.abc.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abc.AbcDao
import io.example.fit.data.abc.OfflineFirstAbcRepository
import io.example.fit.data.abc.api.AbcRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcDataModule {

    @Binds
    fun bindAbcRepository(impl: OfflineFirstAbcRepository): AbcRepository

    companion object {
        @Provides
        fun provideAbcDao(database: AppDatabase): AbcDao = database.abc()
    }
}
