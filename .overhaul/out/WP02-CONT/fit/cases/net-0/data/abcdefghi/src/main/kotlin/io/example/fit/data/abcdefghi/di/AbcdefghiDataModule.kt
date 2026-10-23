package io.example.fit.data.abcdefghi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.abcdefghi.NetworkAbcdefghiRepository
import io.example.fit.data.abcdefghi.api.AbcdefghiRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghiDataModule {

    @Binds
    fun bindAbcdefghiRepository(impl: NetworkAbcdefghiRepository): AbcdefghiRepository
}
