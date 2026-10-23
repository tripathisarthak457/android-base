package com.example.none.data.home.di

import com.example.none.data.home.DefaultHomeRepository
import com.example.none.data.home.api.HomeRepository
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
