package com.example.finalafter.crnoui.data.auth.di

import com.example.finalafter.crnoui.data.auth.DefaultAuthRepository
import com.example.finalafter.crnoui.data.auth.api.AuthRepository
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
