package com.base.app.core.analytics.di

import com.base.app.core.analytics.AnalyticsTracker
import com.base.app.core.analytics.FirebaseAnalyticsTracker
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the analytics vendor.
 *
 * This is the whole of the app's coupling to Firebase Analytics. Swapping vendors, or turning
 * analytics off entirely, replaces this file and touches nothing else — which is the only reason
 * [AnalyticsTracker] is an interface rather than the concrete class.
 */
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
