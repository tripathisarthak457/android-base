package io.example.fit.data.ab.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.ab.AbDao
import io.example.fit.data.ab.OfflineFirstAbRepository
import io.example.fit.data.ab.api.AbRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbDataModule {

    @Binds
    fun bindAbRepository(impl: OfflineFirstAbRepository): AbRepository

    companion object {
        @Provides
        fun provideAbDao(database: AppDatabase): AbDao = database.ab()
    }
}
