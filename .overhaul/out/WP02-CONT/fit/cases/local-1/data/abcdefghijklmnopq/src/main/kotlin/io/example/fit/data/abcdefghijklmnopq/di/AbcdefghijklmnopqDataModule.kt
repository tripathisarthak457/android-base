package io.example.fit.data.abcdefghijklmnopq.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopq.DefaultAbcdefghijklmnopqRepository
import io.example.fit.data.abcdefghijklmnopq.api.AbcdefghijklmnopqRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqDataModule {

    @Binds
    fun bindAbcdefghijklmnopqRepository(
        impl: DefaultAbcdefghijklmnopqRepository,
    ): AbcdefghijklmnopqRepository
}
