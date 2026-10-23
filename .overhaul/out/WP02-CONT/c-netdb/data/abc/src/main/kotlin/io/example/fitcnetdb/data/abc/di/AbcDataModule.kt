package io.example.fitcnetdb.data.abc.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnetdb.core.database.AppDatabase
import io.example.fitcnetdb.core.database.abc.AbcDao
import io.example.fitcnetdb.data.abc.OfflineFirstAbcRepository
import io.example.fitcnetdb.data.abc.api.AbcRepository

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
