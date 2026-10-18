package com.example.wp04.base.data.auth.di

import com.example.wp04.base.data.auth.AuthRepository
import com.example.wp04.base.data.auth.DefaultAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AuthDataModule {
    @Binds
    fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository
}
