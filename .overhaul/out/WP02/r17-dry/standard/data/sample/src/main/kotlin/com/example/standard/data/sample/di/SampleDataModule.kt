package com.example.standard.data.sample.di

import com.example.standard.data.sample.DefaultSampleRepository
import com.example.standard.data.sample.api.SampleRepository
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
