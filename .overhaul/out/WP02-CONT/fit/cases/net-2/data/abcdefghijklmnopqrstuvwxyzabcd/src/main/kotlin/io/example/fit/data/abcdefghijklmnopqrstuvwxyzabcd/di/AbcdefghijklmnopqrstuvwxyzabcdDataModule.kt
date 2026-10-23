package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.NetworkAbcdefghijklmnopqrstuvwxyzabcdRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzabcdDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzabcdRepository(
        impl: NetworkAbcdefghijklmnopqrstuvwxyzabcdRepository,
    ): AbcdefghijklmnopqrstuvwxyzabcdRepository
}
