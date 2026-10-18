package com.example.wp04.abstd.data.auth.di

import com.example.wp04.abstd.data.auth.DefaultAuthRepository
import com.example.wp04.abstd.data.auth.api.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthDataModule {

    @Binds
    fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository
}
