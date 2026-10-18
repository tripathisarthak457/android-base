package com.example.hud.everything.core.analytics.di

import com.example.hud.everything.core.analytics.AnalyticsTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.hud.everything.core.analytics.firebase.FirebaseAnalyticsTracker
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

/** Binds the analytics vendor: the whole of the app's coupling to one. */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(): FirebaseAnalytics = Firebase.analytics

    @Provides
    @Singleton
    fun provideAnalyticsTracker(tracker: FirebaseAnalyticsTracker): AnalyticsTracker = tracker
}
