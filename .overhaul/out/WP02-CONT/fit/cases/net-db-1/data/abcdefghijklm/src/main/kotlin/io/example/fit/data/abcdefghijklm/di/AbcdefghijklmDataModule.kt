package io.example.fit.data.abcdefghijklm.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklm.AbcdefghijklmDao
import io.example.fit.data.abcdefghijklm.OfflineFirstAbcdefghijklmRepository
import io.example.fit.data.abcdefghijklm.api.AbcdefghijklmRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmDataModule {

    @Binds
    fun bindAbcdefghijklmRepository(
        impl: OfflineFirstAbcdefghijklmRepository,
    ): AbcdefghijklmRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmDao(database: AppDatabase): AbcdefghijklmDao =
            database.abcdefghijklm()
    }
}
