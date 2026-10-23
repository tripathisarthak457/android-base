package io.example.fitclocal.data.abcdefghijklmnopqrstuvw.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitclocal.data.abcdefghijklmnopqrstuvw.DefaultAbcdefghijklmnopqrstuvwRepository
import io.example.fitclocal.data.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwRepository(
        impl: DefaultAbcdefghijklmnopqrstuvwRepository,
    ): AbcdefghijklmnopqrstuvwRepository
}
