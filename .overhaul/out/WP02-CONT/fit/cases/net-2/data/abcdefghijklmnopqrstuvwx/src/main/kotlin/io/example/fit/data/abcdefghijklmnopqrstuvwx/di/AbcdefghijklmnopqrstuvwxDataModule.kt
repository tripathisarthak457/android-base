package io.example.fit.data.abcdefghijklmnopqrstuvwx.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvwx.NetworkAbcdefghijklmnopqrstuvwxRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.AbcdefghijklmnopqrstuvwxRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxRepository(
        impl: NetworkAbcdefghijklmnopqrstuvwxRepository,
    ): AbcdefghijklmnopqrstuvwxRepository
}
