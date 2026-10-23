package io.example.fit.data.abcdefghijklmnop.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnop.NetworkAbcdefghijklmnopRepository
import io.example.fit.data.abcdefghijklmnop.api.AbcdefghijklmnopRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopDataModule {

    @Binds
    fun bindAbcdefghijklmnopRepository(
        impl: NetworkAbcdefghijklmnopRepository,
    ): AbcdefghijklmnopRepository
}
