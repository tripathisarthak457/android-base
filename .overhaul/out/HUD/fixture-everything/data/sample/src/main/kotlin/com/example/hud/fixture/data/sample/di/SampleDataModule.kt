package com.example.hud.fixture.data.sample.di

import com.example.hud.fixture.data.sample.api.SampleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.hud.fixture.core.database.AppDatabase
import com.example.hud.fixture.core.database.sample.SampleDao
import com.example.hud.fixture.data.sample.OfflineFirstSampleRepository
import dagger.Provides

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
