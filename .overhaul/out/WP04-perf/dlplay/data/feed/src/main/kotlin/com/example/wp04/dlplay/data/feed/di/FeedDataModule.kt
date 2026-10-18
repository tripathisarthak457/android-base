package com.example.wp04.dlplay.data.feed.di

import com.example.wp04.dlplay.data.feed.DefaultFeedRepository
import com.example.wp04.dlplay.data.feed.api.FeedRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedDataModule {

    @Binds
    fun bindFeedRepository(impl: DefaultFeedRepository): FeedRepository
}
