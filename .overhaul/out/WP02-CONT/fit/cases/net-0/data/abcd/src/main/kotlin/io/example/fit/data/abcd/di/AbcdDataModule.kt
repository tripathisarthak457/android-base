package io.example.fit.data.abcd.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcd.NetworkAbcdRepository
import io.example.fit.data.abcd.api.AbcdRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdDataModule {

    @Binds
    fun bindAbcdRepository(impl: NetworkAbcdRepository): AbcdRepository
}
