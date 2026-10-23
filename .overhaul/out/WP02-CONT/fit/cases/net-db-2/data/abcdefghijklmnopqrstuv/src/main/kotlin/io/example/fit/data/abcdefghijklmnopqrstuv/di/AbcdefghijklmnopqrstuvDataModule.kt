package io.example.fit.data.abcdefghijklmnopqrstuv.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuv.AbcdefghijklmnopqrstuvDao
import io.example.fit.data.abcdefghijklmnopqrstuv.OfflineFirstAbcdefghijklmnopqrstuvRepository
import io.example.fit.data.abcdefghijklmnopqrstuv.api.AbcdefghijklmnopqrstuvRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvRepository,
    ): AbcdefghijklmnopqrstuvRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvDao(database: AppDatabase): AbcdefghijklmnopqrstuvDao =
            database.abcdefghijklmnopqrstuv()
    }
}
