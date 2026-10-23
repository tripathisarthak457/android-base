package io.example.fitcnet.data.abc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnet.data.abc.NetworkAbcRepository
import io.example.fitcnet.data.abc.api.AbcRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcDataModule {

    @Binds
    fun bindAbcRepository(impl: NetworkAbcRepository): AbcRepository
}
