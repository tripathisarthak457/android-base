package com.example.cicheck.data.orders.di

import com.example.cicheck.data.orders.DefaultOrdersRepository
import com.example.cicheck.data.orders.OrdersRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface OrdersDataModule {

    @Binds
    fun bindOrdersRepository(impl: DefaultOrdersRepository): OrdersRepository
}
