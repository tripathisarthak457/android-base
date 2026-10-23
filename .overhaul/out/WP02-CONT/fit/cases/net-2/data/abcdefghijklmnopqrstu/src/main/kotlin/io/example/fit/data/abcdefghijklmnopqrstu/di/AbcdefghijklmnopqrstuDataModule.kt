package io.example.fit.data.abcdefghijklmnopqrstu.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstu.NetworkAbcdefghijklmnopqrstuRepository
import io.example.fit.data.abcdefghijklmnopqrstu.api.AbcdefghijklmnopqrstuRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuRepository(
        impl: NetworkAbcdefghijklmnopqrstuRepository,
    ): AbcdefghijklmnopqrstuRepository
}
