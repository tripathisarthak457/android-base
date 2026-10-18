package com.example.hud.everything.data.feed.di

import com.example.hud.everything.data.feed.NetworkFeedRepository
import com.example.hud.everything.data.feed.api.FeedRepository
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
