package com.example.lean.data.home.di

import com.example.lean.data.home.DefaultHomeRepository
import com.example.lean.data.home.api.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface HomeDataModule {

    @Binds
    fun bindHomeRepository(impl: DefaultHomeRepository): HomeRepository
}
