package io.example.fit.data.abc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abc.DefaultAbcRepository
import io.example.fit.data.abc.api.AbcRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcDataModule {

    @Binds
    fun bindAbcRepository(impl: DefaultAbcRepository): AbcRepository
}
