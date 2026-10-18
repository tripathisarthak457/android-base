package com.example.wp04.abstd.di

import com.example.wp04.abstd.core.common.time.AppClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {

    @Provides
    fun provideClock(): AppClock = AppClock.System
}
