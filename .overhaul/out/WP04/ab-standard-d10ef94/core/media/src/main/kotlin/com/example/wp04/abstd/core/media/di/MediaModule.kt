package com.example.wp04.abstd.core.media.di

import com.example.wp04.abstd.core.media.PassthroughVideoTranscoder
import com.example.wp04.abstd.core.media.VideoTranscoder
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
