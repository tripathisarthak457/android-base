package io.example.fit.data.abcde.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcde.DefaultAbcdeRepository
import io.example.fit.data.abcde.api.AbcdeRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdeDataModule {

    @Binds
    fun bindAbcdeRepository(impl: DefaultAbcdeRepository): AbcdeRepository
}
