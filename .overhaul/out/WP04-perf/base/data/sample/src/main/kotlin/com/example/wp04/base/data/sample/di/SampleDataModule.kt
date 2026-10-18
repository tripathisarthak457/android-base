package com.example.wp04.base.data.sample.di

import com.example.wp04.base.data.sample.DefaultSampleRepository
import com.example.wp04.base.data.sample.SampleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SampleDataModule {
    @Binds
    fun bindSampleRepository(impl: DefaultSampleRepository): SampleRepository
}
