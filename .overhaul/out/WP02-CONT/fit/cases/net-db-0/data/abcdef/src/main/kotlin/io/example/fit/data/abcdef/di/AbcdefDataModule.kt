package io.example.fit.data.abcdef.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdef.AbcdefDao
import io.example.fit.data.abcdef.OfflineFirstAbcdefRepository
import io.example.fit.data.abcdef.api.AbcdefRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefDataModule {

    @Binds
    fun bindAbcdefRepository(impl: OfflineFirstAbcdefRepository): AbcdefRepository

    companion object {
        @Provides
        fun provideAbcdefDao(database: AppDatabase): AbcdefDao = database.abcdef()
    }
}
