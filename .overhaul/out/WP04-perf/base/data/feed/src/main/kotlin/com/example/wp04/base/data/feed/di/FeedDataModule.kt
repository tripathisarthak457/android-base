package com.example.wp04.base.data.feed.di

import com.example.wp04.base.data.feed.DefaultFeedRepository
import com.example.wp04.base.data.feed.FeedRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FeedDataModule {
    @Binds
    fun bindFeedRepository(impl: DefaultFeedRepository): FeedRepository
}
