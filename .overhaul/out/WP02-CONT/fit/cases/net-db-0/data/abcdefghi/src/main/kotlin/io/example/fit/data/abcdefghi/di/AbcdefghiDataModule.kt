package io.example.fit.data.abcdefghi.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghi.AbcdefghiDao
import io.example.fit.data.abcdefghi.OfflineFirstAbcdefghiRepository
import io.example.fit.data.abcdefghi.api.AbcdefghiRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghiDataModule {

    @Binds
    fun bindAbcdefghiRepository(impl: OfflineFirstAbcdefghiRepository): AbcdefghiRepository

    companion object {
        @Provides
        fun provideAbcdefghiDao(database: AppDatabase): AbcdefghiDao = database.abcdefghi()
    }
}
