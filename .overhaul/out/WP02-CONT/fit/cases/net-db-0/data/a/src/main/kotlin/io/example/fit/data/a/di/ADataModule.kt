package io.example.fit.data.a.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.a.ADao
import io.example.fit.data.a.OfflineFirstARepository
import io.example.fit.data.a.api.ARepository

@Module
@InstallIn(SingletonComponent::class)
internal interface ADataModule {

    @Binds
    fun bindARepository(impl: OfflineFirstARepository): ARepository

    companion object {
        @Provides
        fun provideADao(database: AppDatabase): ADao = database.a()
    }
}
