package io.example.fit.data.abcdefghijkl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijkl.DefaultAbcdefghijklRepository
import io.example.fit.data.abcdefghijkl.api.AbcdefghijklRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklDataModule {

    @Binds
    fun bindAbcdefghijklRepository(impl: DefaultAbcdefghijklRepository): AbcdefghijklRepository
}
