package io.example.fit.data.abcdefghijk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijk.DefaultAbcdefghijkRepository
import io.example.fit.data.abcdefghijk.api.AbcdefghijkRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijkDataModule {

    @Binds
    fun bindAbcdefghijkRepository(impl: DefaultAbcdefghijkRepository): AbcdefghijkRepository
}
