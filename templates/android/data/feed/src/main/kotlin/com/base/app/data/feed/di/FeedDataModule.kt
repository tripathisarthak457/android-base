package com.base.app.data.feed.di

import com.base.app.data.feed.DefaultFeedRepository
import com.base.app.data.feed.FeedRepository
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
