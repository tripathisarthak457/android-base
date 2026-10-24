package com.example.deskstandard.data.sample.di

import com.example.deskstandard.core.database.AppDatabase
import com.example.deskstandard.core.database.sample.SampleDao
import com.example.deskstandard.data.sample.OfflineFirstSampleRepository
import com.example.deskstandard.data.sample.api.SampleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SampleDataModule {
    @Binds
    fun bindSampleRepository(impl: OfflineFirstSampleRepository): SampleRepository

    companion object {
        @Provides
        fun provideSampleDao(database: AppDatabase): SampleDao = database.samples()
    }
}
