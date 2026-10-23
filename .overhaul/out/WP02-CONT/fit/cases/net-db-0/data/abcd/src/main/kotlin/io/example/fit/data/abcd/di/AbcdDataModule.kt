package io.example.fit.data.abcd.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcd.AbcdDao
import io.example.fit.data.abcd.OfflineFirstAbcdRepository
import io.example.fit.data.abcd.api.AbcdRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdDataModule {

    @Binds
    fun bindAbcdRepository(impl: OfflineFirstAbcdRepository): AbcdRepository

    companion object {
        @Provides
        fun provideAbcdDao(database: AppDatabase): AbcdDao = database.abcd()
    }
}
