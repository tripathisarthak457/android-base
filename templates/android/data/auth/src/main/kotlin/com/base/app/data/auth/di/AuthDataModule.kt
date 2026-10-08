package com.base.app.data.auth.di

import com.base.app.data.auth.AuthRepository
import com.base.app.data.auth.DefaultAuthRepository
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
