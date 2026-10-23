package io.example.fit.data.ab.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.ab.NetworkAbRepository
import io.example.fit.data.ab.api.AbRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbDataModule {

    @Binds
    fun bindAbRepository(impl: NetworkAbRepository): AbRepository
}
