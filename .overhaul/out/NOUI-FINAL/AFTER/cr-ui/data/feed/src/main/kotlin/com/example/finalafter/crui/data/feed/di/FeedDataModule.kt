package com.example.finalafter.crui.data.feed.di

import com.example.finalafter.crui.data.feed.NetworkFeedRepository
import com.example.finalafter.crui.data.feed.api.FeedRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedDataModule {

    @Binds
    fun bindFeedRepository(impl: NetworkFeedRepository): FeedRepository
}
