package io.example.fit.data.abcdefghijklmnopqr.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqr.NetworkAbcdefghijklmnopqrRepository
import io.example.fit.data.abcdefghijklmnopqr.api.AbcdefghijklmnopqrRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrRepository(
        impl: NetworkAbcdefghijklmnopqrRepository,
    ): AbcdefghijklmnopqrRepository
}
