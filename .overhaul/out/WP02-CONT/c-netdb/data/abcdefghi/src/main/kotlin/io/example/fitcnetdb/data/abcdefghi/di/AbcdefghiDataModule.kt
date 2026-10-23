package io.example.fitcnetdb.data.abcdefghi.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnetdb.core.database.AppDatabase
import io.example.fitcnetdb.core.database.abcdefghi.AbcdefghiDao
import io.example.fitcnetdb.data.abcdefghi.OfflineFirstAbcdefghiRepository
import io.example.fitcnetdb.data.abcdefghi.api.AbcdefghiRepository

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
