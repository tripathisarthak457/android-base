package com.example.hud.lean.data.sample.di

import com.example.hud.lean.data.sample.api.SampleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.hud.lean.data.sample.NetworkSampleRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface SampleDataModule {
        @Binds
        fun bindSampleRepository(impl: NetworkSampleRepository): SampleRepository
}
