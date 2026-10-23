package io.example.fit.data.abcdefghijklmnopqrstu.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fit.core.database.AppDatabase
import io.example.fit.core.database.abcdefghijklmnopqrstu.AbcdefghijklmnopqrstuDao
import io.example.fit.data.abcdefghijklmnopqrstu.OfflineFirstAbcdefghijklmnopqrstuRepository
import io.example.fit.data.abcdefghijklmnopqrstu.api.AbcdefghijklmnopqrstuRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AbcdefghijklmnopqrstuDataModule {

    @Binds
    fun bindAbcdefghijklmnopqrstuRepository(
        impl: OfflineFirstAbcdefghijklmnopqrstuRepository,
    ): AbcdefghijklmnopqrstuRepository

    companion object {
        @Provides
        fun provideAbcdefghijklmnopqrstuDao(database: AppDatabase): AbcdefghijklmnopqrstuDao =
            database.abcdefghijklmnopqrstu()
    }
}
