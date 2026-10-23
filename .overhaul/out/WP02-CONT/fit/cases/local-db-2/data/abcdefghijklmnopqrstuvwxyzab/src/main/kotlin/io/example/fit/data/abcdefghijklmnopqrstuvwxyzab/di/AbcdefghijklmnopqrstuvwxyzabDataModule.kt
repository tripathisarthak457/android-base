package io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.DefaultAbcdefghijklmnopqrstuvwxyzabRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.AbcdefghijklmnopqrstuvwxyzabRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuvwxyzabDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuvwxyzabRepository(
        impl: DefaultAbcdefghijklmnopqrstuvwxyzabRepository,
    ): AbcdefghijklmnopqrstuvwxyzabRepository
}
