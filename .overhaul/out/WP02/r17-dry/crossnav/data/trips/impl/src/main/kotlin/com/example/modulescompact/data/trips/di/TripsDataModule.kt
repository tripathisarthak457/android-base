package com.example.modulescompact.data.trips.di

import com.example.modulescompact.data.trips.DefaultTripsRepository
import com.example.modulescompact.data.trips.api.TripsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TripsDataModule {

    @Binds
    fun bindTripsRepository(impl: DefaultTripsRepository): TripsRepository
}
