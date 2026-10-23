package io.example.fit.data.abcdefghijklmnopqrst.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrst.NetworkAbcdefghijklmnopqrstRepository
import io.example.fit.data.abcdefghijklmnopqrst.api.AbcdefghijklmnopqrstRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstRepository(
        impl: NetworkAbcdefghijklmnopqrstRepository,
    ): AbcdefghijklmnopqrstRepository
}
