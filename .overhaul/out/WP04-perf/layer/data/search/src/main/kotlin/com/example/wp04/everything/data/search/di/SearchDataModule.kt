package com.example.wp04.everything.data.search.di

import com.example.wp04.everything.data.search.DefaultSearchRepository
import com.example.wp04.everything.data.search.api.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SearchDataModule {

    @Binds
    fun bindSearchRepository(impl: DefaultSearchRepository): SearchRepository
}
