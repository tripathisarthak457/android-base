package io.example.fit.data.a.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.data.a.DefaultARepository
import io.example.fit.data.a.api.ARepository

@Module
@InstallIn(SingletonComponent::class)
internal interface ADataModule {

    @Binds
    fun bindARepository(impl: DefaultARepository): ARepository
}
