package io.example.fit.data.abcdefghij.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghij.NetworkAbcdefghijRepository
import io.example.fit.data.abcdefghij.api.AbcdefghijRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijDataModule {

    @Binds
    fun bindAbcdefghijRepository(impl: NetworkAbcdefghijRepository): AbcdefghijRepository
}
