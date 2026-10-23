package io.example.fit.data.abcdefghijklmnopqrstuvw.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuvw.AbcdefghijklmnopqrstuvwDao
import io.example.fit.data.abcdefghijklmnopqrstuvw.OfflineFirstAbcdefghijklmnopqrstuvwRepository
import io.example.fit.data.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwRepository,
    ): AbcdefghijklmnopqrstuvwRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwDao(database: AppDatabase): AbcdefghijklmnopqrstuvwDao =
            database.abcdefghijklmnopqrstuvw()
    }
}
