package com.example.finalafter.crnoui.data.sample.di

import com.example.finalafter.crnoui.core.database.AppDatabase
import com.example.finalafter.crnoui.core.database.sample.SampleDao
import com.example.finalafter.crnoui.data.sample.OfflineFirstSampleRepository
import com.example.finalafter.crnoui.data.sample.api.SampleRepository
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
