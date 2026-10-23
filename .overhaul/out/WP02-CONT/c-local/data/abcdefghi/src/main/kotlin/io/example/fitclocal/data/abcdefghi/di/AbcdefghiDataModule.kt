package io.example.fitclocal.data.abcdefghi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitclocal.data.abcdefghi.DefaultAbcdefghiRepository
import io.example.fitclocal.data.abcdefghi.api.AbcdefghiRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghiDataModule {

    @Binds
    fun bindAbcdefghiRepository(impl: DefaultAbcdefghiRepository): AbcdefghiRepository
}
