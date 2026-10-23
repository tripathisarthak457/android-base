package io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyzab.AbcdefghijklmnopqrstuvwxyzabDao
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.OfflineFirstAbcdefghijklmnopqrstuvwxyzabRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.AbcdefghijklmnopqrstuvwxyzabRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzabDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzabRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuvwxyzabRepository,
    ): AbcdefghijklmnopqrstuvwxyzabRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuvwxyzabDao(
            database: AppDatabase,
        ): AbcdefghijklmnopqrstuvwxyzabDao = database.abcdefghijklmnopqrstuvwxyzab()
    }
}
