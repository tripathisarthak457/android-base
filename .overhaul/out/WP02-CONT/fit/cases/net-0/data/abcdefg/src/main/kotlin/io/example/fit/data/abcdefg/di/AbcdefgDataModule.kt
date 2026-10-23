package io.example.fit.data.abcdefg.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefg.NetworkAbcdefgRepository
import io.example.fit.data.abcdefg.api.AbcdefgRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefgDataModule {

    @Binds
    fun bindAbcdefgRepository(impl: NetworkAbcdefgRepository): AbcdefgRepository
}
