package io.example.fit.data.abcdefghijklmnopqrstuvwxy.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuvwxy.AbcdefghijklmnopqrstuvwxyDao
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.OfflineFirstAbcdefghijklmnopqrstuvwxyRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.AbcdefghijklmnopqrstuvwxyRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwxyRepository,
    ): AbcdefghijklmnopqrstuvwxyRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwxyDao(
            database: AppDatabase,
        ): AbcdefghijklmnopqrstuvwxyDao = database.abcdefghijklmnopqrstuvwxy()
    }
}
