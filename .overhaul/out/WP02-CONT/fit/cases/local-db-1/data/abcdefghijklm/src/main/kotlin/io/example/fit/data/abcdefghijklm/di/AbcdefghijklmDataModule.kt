package io.example.fit.data.abcdefghijklm.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklm.DefaultAbcdefghijklmRepository
import io.example.fit.data.abcdefghijklm.api.AbcdefghijklmRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmDataModule {

    @Binds
    fun bindAbcdefghijklmRepository(impl: DefaultAbcdefghijklmRepository): AbcdefghijklmRepository
}
