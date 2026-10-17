package com.example.modulescompact.data.wallet.di

import com.example.modulescompact.data.wallet.DefaultWalletRepository
import com.example.modulescompact.data.wallet.api.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletDataModule {

    @Binds
    fun bindWalletRepository(impl: DefaultWalletRepository): WalletRepository
}
