package com.example.wp04.base.data.search.di

import com.example.wp04.base.data.search.DefaultSearchRepository
import com.example.wp04.base.data.search.SearchRepository
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
