package io.example.fit.data.abcdefghijklmnopqrs.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrs.AbcdefghijklmnopqrsDao
import io.example.fit.data.abcdefghijklmnopqrs.OfflineFirstAbcdefghijklmnopqrsRepository
import io.example.fit.data.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrsDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrsRepository(
        impl: OfflineFirstAbcdefghijklmnopqrsRepository,
    ): AbcdefghijklmnopqrsRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrsDao(database: AppDatabase): AbcdefghijklmnopqrsDao =
            database.abcdefghijklmnopqrs()
    }
}
