package com.base.app.data.search.di

import com.base.app.data.search.DefaultSearchRepository
import com.base.app.data.search.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SearchDataModule {

    @Binds
    fun bindSearchRepository(impl: DefaultSearchRepository): SearchRepository
}
