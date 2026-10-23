package io.example.fit.data.abcdefghijklmnopqrstuvwxyz.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyz.AbcdefghijklmnopqrstuvwxyzDao
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.OfflineFirstAbcdefghijklmnopqrstuvwxyzRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.AbcdefghijklmnopqrstuvwxyzRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwxyzRepository,
    ): AbcdefghijklmnopqrstuvwxyzRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwxyzDao(
            database: AppDatabase,
        ): AbcdefghijklmnopqrstuvwxyzDao = database.abcdefghijklmnopqrstuvwxyz()
    }
}
