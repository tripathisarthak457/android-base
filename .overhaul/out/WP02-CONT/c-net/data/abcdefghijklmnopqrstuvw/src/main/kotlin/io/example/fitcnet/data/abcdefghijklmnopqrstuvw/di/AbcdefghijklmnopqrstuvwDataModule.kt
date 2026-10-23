package io.example.fitcnet.data.abcdefghijklmnopqrstuvw.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.NetworkAbcdefghijklmnopqrstuvwRepository
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwRepository(
        impl: NetworkAbcdefghijklmnopqrstuvwRepository,
    ): AbcdefghijklmnopqrstuvwRepository
}
