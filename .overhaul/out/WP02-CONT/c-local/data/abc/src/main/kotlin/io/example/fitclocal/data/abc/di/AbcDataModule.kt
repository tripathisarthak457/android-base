package io.example.fitclocal.data.abc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitclocal.data.abc.DefaultAbcRepository
import io.example.fitclocal.data.abc.api.AbcRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcDataModule {

    @Binds
    fun bindAbcRepository(impl: DefaultAbcRepository): AbcRepository
}
