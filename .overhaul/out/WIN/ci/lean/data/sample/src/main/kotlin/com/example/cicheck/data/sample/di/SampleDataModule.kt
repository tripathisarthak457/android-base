package com.example.cicheck.data.sample.di

import com.example.cicheck.data.sample.DefaultSampleRepository
import com.example.cicheck.data.sample.api.SampleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SampleDataModule {

    @Binds
    fun bindSampleRepository(impl: DefaultSampleRepository): SampleRepository
}
