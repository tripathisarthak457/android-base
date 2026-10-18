package com.example.hud.everything.data.sample.di

import com.example.hud.everything.data.sample.api.SampleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.hud.everything.core.database.AppDatabase
import com.example.hud.everything.core.database.sample.SampleDao
import com.example.hud.everything.data.sample.OfflineFirstSampleRepository
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
