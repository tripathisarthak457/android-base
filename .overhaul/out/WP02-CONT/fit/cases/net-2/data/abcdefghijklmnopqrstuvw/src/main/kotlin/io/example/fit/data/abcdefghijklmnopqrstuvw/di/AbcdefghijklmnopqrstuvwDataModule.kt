package io.example.fit.data.abcdefghijklmnopqrstuvw.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvw.NetworkAbcdefghijklmnopqrstuvwRepository
import io.example.fit.data.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwRepository(
        impl: NetworkAbcdefghijklmnopqrstuvwRepository,
    ): AbcdefghijklmnopqrstuvwRepository
}
