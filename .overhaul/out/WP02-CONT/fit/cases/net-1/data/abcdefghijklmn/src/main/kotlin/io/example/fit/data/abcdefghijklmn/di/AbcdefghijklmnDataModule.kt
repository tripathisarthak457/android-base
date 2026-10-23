package io.example.fit.data.abcdefghijklmn.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmn.NetworkAbcdefghijklmnRepository
import io.example.fit.data.abcdefghijklmn.api.AbcdefghijklmnRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnDataModule {

    @Binds
    fun bindAbcdefghijklmnRepository(
        impl: NetworkAbcdefghijklmnRepository,
    ): AbcdefghijklmnRepository
}
