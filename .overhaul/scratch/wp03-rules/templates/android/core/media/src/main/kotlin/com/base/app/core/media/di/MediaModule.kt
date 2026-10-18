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

    /** Passes video through unchanged. Bind a real [VideoTranscoder] here when you need one. */
    @Binds
    fun bindVideoTranscoder(impl: PassthroughVideoTranscoder): VideoTranscoder
}
