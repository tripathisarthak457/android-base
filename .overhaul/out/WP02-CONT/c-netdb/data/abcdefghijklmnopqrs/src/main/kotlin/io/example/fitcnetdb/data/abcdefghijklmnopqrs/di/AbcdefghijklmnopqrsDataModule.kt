package io.example.fitcnetdb.data.abcdefghijklmnopqrs.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnetdb.core.database.AppDatabase
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrs.AbcdefghijklmnopqrsDao
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.OfflineFirstAbcdefghijklmnopqrsRepository
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsRepository

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
