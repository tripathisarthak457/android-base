package io.example.fit.data.abcdefghijklmnopqrstuvwxyza.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.DefaultAbcdefghijklmnopqrstuvwxyzaRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.api.AbcdefghijklmnopqrstuvwxyzaRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzaDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzaRepository(
        impl: DefaultAbcdefghijklmnopqrstuvwxyzaRepository,
    ): AbcdefghijklmnopqrstuvwxyzaRepository
}
