package com.base.app.core.analytics.di

import com.base.app.core.analytics.AnalyticsTracker
// <opt:!firebaseanalytics>import com.base.app.core.analytics.NoOpAnalyticsTracker
// <opt:firebaseanalytics>
import com.base.app.core.analytics.firebase.FirebaseAnalyticsTracker
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
// </opt:firebaseanalytics>
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the analytics vendor: the whole of the app's coupling to one. */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    // <opt:firebaseanalytics>

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(): FirebaseAnalytics = Firebase.analytics

    @Provides
    @Singleton
    fun provideAnalyticsTracker(tracker: FirebaseAnalyticsTracker): AnalyticsTracker = tracker
    // </opt:firebaseanalytics>
    // <opt-else:firebaseanalytics>
    //~
    //~     @Provides
    //~     @Singleton
    //~     fun provideAnalyticsTracker(): AnalyticsTracker = NoOpAnalyticsTracker()
    // </opt-else:firebaseanalytics>
}
