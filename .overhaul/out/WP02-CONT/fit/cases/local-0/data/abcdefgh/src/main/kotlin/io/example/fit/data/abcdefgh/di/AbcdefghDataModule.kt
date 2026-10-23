package io.example.fit.data.abcdefgh.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefgh.DefaultAbcdefghRepository
import io.example.fit.data.abcdefgh.api.AbcdefghRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghDataModule {

    @Binds
    fun bindAbcdefghRepository(impl: DefaultAbcdefghRepository): AbcdefghRepository
}
