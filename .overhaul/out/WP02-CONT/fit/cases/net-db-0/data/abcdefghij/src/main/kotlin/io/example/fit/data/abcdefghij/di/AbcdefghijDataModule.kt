package io.example.fit.data.abcdefghij.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghij.AbcdefghijDao
import io.example.fit.data.abcdefghij.OfflineFirstAbcdefghijRepository
import io.example.fit.data.abcdefghij.api.AbcdefghijRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijDataModule {

    @Binds
    fun bindAbcdefghijRepository(impl: OfflineFirstAbcdefghijRepository): AbcdefghijRepository

    companion object {
        @Provides
        fun provideAbcdefghijDao(database: AppDatabase): AbcdefghijDao = database.abcdefghij()
    }
}
