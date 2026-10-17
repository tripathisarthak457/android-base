package com.example.modulesmodular.data.orders.di

import com.example.modulesmodular.data.orders.DefaultOrdersRepository
import com.example.modulesmodular.data.orders.api.OrdersRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface OrdersDataModule {

    @Binds
    fun bindOrdersRepository(impl: DefaultOrdersRepository): OrdersRepository
}
