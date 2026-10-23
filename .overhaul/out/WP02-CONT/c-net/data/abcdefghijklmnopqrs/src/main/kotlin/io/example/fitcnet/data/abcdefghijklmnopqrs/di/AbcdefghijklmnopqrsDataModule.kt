package io.example.fitcnet.data.abcdefghijklmnopqrs.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnet.data.abcdefghijklmnopqrs.NetworkAbcdefghijklmnopqrsRepository
import io.example.fitcnet.data.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrsDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrsRepository(
        impl: NetworkAbcdefghijklmnopqrsRepository,
    ): AbcdefghijklmnopqrsRepository
}
