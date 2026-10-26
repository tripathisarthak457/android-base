package com.example.finalafter.crnoui.di

import com.example.finalafter.crnoui.core.common.time.AppClock
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
