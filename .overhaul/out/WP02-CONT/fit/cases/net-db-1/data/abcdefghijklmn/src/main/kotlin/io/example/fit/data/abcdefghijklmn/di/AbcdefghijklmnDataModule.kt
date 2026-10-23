package io.example.fit.data.abcdefghijklmn.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmn.AbcdefghijklmnDao
import io.example.fit.data.abcdefghijklmn.OfflineFirstAbcdefghijklmnRepository
import io.example.fit.data.abcdefghijklmn.api.AbcdefghijklmnRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnDataModule {

    @Binds
    fun bindAbcdefghijklmnRepository(
        impl: OfflineFirstAbcdefghijklmnRepository,
    ): AbcdefghijklmnRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnDao(database: AppDatabase): AbcdefghijklmnDao =
            database.abcdefghijklmn()
    }
}
