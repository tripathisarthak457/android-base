package io.example.fit.data.abcdefghijklmnopqrs.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrs.DefaultAbcdefghijklmnopqrsRepository
import io.example.fit.data.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrsDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrsRepository(
        impl: DefaultAbcdefghijklmnopqrsRepository,
    ): AbcdefghijklmnopqrsRepository
}
