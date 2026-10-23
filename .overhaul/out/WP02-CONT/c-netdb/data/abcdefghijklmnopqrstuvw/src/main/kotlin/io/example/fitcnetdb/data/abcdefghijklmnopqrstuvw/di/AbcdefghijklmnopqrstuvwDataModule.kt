package io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnetdb.core.database.AppDatabase
import io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvw.AbcdefghijklmnopqrstuvwDao
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.OfflineFirstAbcdefghijklmnopqrstuvwRepository
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwRepository

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
