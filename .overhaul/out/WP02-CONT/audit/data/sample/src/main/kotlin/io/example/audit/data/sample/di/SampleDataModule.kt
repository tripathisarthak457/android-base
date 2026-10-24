package io.example.audit.data.sample.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.audit.core.database.AppDatabase
import io.example.audit.core.database.sample.SampleDao
import io.example.audit.data.sample.OfflineFirstSampleRepository
import io.example.audit.data.sample.api.SampleRepository

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
