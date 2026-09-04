package com.base.app.core.media.di

import com.base.app.core.media.PassthroughVideoTranscoder
import com.base.app.core.media.VideoTranscoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {

    /**
     * The default hands video through unchanged and logs when it should not have.
     *
     * Replace this binding with a real transcoder when you need one — see [VideoTranscoder] for
     * why a starter does not ship one. Nothing else in the app changes.
     */
    @Binds
    fun bindVideoTranscoder(impl: PassthroughVideoTranscoder): VideoTranscoder
}
