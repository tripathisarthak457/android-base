package com.base.app.data.sample.di

import com.base.app.data.sample.DefaultSampleRepository
import com.base.app.data.sample.SampleRepository
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
