package com.example.cmp.data.home.di

import com.example.cmp.data.home.DefaultHomeRepository
import com.example.cmp.data.home.api.HomeRepository
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
