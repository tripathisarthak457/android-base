package io.example.fit.data.abcdefghijklmnopqrstuv.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuv.DefaultAbcdefghijklmnopqrstuvRepository
import io.example.fit.data.abcdefghijklmnopqrstuv.api.AbcdefghijklmnopqrstuvRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvRepository(
        impl: DefaultAbcdefghijklmnopqrstuvRepository,
    ): AbcdefghijklmnopqrstuvRepository
}
