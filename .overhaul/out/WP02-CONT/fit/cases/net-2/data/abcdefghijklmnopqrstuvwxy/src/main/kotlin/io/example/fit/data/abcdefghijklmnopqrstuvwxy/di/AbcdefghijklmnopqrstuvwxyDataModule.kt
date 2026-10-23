package io.example.fit.data.abcdefghijklmnopqrstuvwxy.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.NetworkAbcdefghijklmnopqrstuvwxyRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.AbcdefghijklmnopqrstuvwxyRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyRepository(
        impl: NetworkAbcdefghijklmnopqrstuvwxyRepository,
    ): AbcdefghijklmnopqrstuvwxyRepository
}
