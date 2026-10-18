package com.example.wp04.base.data.orders.di

import com.example.wp04.base.data.orders.DefaultOrdersRepository
import com.example.wp04.base.data.orders.OrdersRepository
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
