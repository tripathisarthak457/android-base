package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.NetworkAbcdefghijklmnopqrstuvwxyzabcRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.AbcdefghijklmnopqrstuvwxyzabcRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzabcDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzabcRepository(
        impl: NetworkAbcdefghijklmnopqrstuvwxyzabcRepository,
    ): AbcdefghijklmnopqrstuvwxyzabcRepository
}
