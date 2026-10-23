package io.example.fitclocal.data.abcdefghijklmno.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitclocal.data.abcdefghijklmno.DefaultAbcdefghijklmnoRepository
import io.example.fitclocal.data.abcdefghijklmno.api.AbcdefghijklmnoRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnoDataModule {

    @Binds
    fun bindAbcdefghijklmnoRepository(
        impl: DefaultAbcdefghijklmnoRepository,
    ): AbcdefghijklmnoRepository
}
