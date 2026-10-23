package io.example.fit.data.abcdefg.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefg.AbcdefgDao
import io.example.fit.data.abcdefg.OfflineFirstAbcdefgRepository
import io.example.fit.data.abcdefg.api.AbcdefgRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefgDataModule {

    @Binds
    fun bindAbcdefgRepository(impl: OfflineFirstAbcdefgRepository): AbcdefgRepository

    companion object {
        @Provides
        fun provideAbcdefgDao(database: AppDatabase): AbcdefgDao = database.abcdefg()
    }
}
