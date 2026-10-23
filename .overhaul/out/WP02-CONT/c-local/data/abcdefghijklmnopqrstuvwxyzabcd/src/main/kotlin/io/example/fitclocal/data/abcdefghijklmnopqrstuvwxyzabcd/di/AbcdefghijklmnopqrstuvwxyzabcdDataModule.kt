package io.example.fitclocal.data.abcdefghijklmnopqrstuvwxyzabcd.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitclocal.data.abcdefghijklmnopqrstuvwxyzabcd.DefaultAbcdefghijklmnopqrstuvwxyzabcdRepository
import io.example.fitclocal.data.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzabcdDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzabcdRepository(
        impl: DefaultAbcdefghijklmnopqrstuvwxyzabcdRepository,
    ): AbcdefghijklmnopqrstuvwxyzabcdRepository
}
