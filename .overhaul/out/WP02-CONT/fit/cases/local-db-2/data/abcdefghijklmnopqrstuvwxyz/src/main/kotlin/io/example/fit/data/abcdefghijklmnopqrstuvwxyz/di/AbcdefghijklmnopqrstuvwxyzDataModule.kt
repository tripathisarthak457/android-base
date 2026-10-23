package io.example.fit.data.abcdefghijklmnopqrstuvwxyz.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.DefaultAbcdefghijklmnopqrstuvwxyzRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.AbcdefghijklmnopqrstuvwxyzRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzRepository(
        impl: DefaultAbcdefghijklmnopqrstuvwxyzRepository,
    ): AbcdefghijklmnopqrstuvwxyzRepository
}
