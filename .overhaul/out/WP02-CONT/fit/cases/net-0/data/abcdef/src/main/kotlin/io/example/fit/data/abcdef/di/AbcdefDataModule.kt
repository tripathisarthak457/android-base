package io.example.fit.data.abcdef.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdef.NetworkAbcdefRepository
import io.example.fit.data.abcdef.api.AbcdefRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefDataModule {

    @Binds
    fun bindAbcdefRepository(impl: NetworkAbcdefRepository): AbcdefRepository
}
