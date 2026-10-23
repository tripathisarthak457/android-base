package io.example.fit.data.abcdefghijklmno.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmno.AbcdefghijklmnoDao
import io.example.fit.data.abcdefghijklmno.OfflineFirstAbcdefghijklmnoRepository
import io.example.fit.data.abcdefghijklmno.api.AbcdefghijklmnoRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnoDataModule {

    @Binds
    fun bindAbcdefghijklmnoRepository(
        impl: OfflineFirstAbcdefghijklmnoRepository,
    ): AbcdefghijklmnoRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnoDao(database: AppDatabase): AbcdefghijklmnoDao =
            database.abcdefghijklmno()
    }
}
