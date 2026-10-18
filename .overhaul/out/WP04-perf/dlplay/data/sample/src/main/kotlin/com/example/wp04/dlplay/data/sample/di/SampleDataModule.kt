package com.example.wp04.dlplay.data.sample.di

import com.example.wp04.dlplay.data.sample.DefaultSampleRepository
import com.example.wp04.dlplay.data.sample.api.SampleRepository
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
