package com.example.wp04.base.data.wallet.di

import com.example.wp04.base.data.wallet.DefaultWalletRepository
import com.example.wp04.base.data.wallet.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface WalletDataModule {
    @Binds
    fun bindWalletRepository(impl: DefaultWalletRepository): WalletRepository
}
