package io.example.fit.data.abcdefghijkl.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijkl.AbcdefghijklDao
import io.example.fit.data.abcdefghijkl.OfflineFirstAbcdefghijklRepository
import io.example.fit.data.abcdefghijkl.api.AbcdefghijklRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklDataModule {

    @Binds
    fun bindAbcdefghijklRepository(impl: OfflineFirstAbcdefghijklRepository): AbcdefghijklRepository

    companion object {
        @Provides
        fun provideAbcdefghijklDao(database: AppDatabase): AbcdefghijklDao = database.abcdefghijkl()
    }
}
