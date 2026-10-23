package io.example.fitcnet.data.abcdefghijklmno.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnet.data.abcdefghijklmno.NetworkAbcdefghijklmnoRepository
import io.example.fitcnet.data.abcdefghijklmno.api.AbcdefghijklmnoRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnoDataModule {

    @Binds
    fun bindAbcdefghijklmnoRepository(
        impl: NetworkAbcdefghijklmnoRepository,
    ): AbcdefghijklmnoRepository
}
