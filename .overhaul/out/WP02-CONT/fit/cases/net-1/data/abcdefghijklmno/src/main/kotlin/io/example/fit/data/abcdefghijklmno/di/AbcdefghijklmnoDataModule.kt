package io.example.fit.data.abcdefghijklmno.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmno.NetworkAbcdefghijklmnoRepository
import io.example.fit.data.abcdefghijklmno.api.AbcdefghijklmnoRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnoDataModule {

    @Binds
    fun bindAbcdefghijklmnoRepository(
        impl: NetworkAbcdefghijklmnoRepository,
    ): AbcdefghijklmnoRepository
}
