package io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.audit.core.database.AppDatabase
import io.example.audit.core.database.abcdefghijklmnopqrstuvwxyzabcd.AbcdefghijklmnopqrstuvwxyzabcdDao
import io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.OfflineFirstAbcdefghijklmnopqrstuvwxyzabcdRepository
import io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzabcdDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzabcdRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwxyzabcdRepository,
    ): AbcdefghijklmnopqrstuvwxyzabcdRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwxyzabcdDao(
            database: AppDatabase,
        ): AbcdefghijklmnopqrstuvwxyzabcdDao = database.abcdefghijklmnopqrstuvwxyzabcd()
    }
}
