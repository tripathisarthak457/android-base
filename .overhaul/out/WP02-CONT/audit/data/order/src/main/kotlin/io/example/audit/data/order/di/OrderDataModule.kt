package io.example.audit.data.order.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.audit.core.database.AppDatabase
import io.example.audit.core.database.order.OrderDao
import io.example.audit.data.order.OfflineFirstOrderRepository
import io.example.audit.data.order.api.OrderRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface OrderDataModule {

    @Binds
    fun bindOrderRepository(impl: OfflineFirstOrderRepository): OrderRepository

    companion object {
        @Provides
        fun provideOrderDao(database: AppDatabase): OrderDao = database.order()
    }
}
