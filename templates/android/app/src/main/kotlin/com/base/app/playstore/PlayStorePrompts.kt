package com.base.app.playstore

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.base.app.core.common.util.AppLogger
import com.base.app.core.datastore.di.SettingsDataStore
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.ktx.requestUpdateFlow
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Offers the user a newer build, without taking the app away from them. */
@Singleton
class AppUpdates @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun updates(): Flow<AppUpdateResult> =
        AppUpdateManagerFactory.create(context)
            .requestUpdateFlow()
            .catch { error ->
                AppLogger.w("Update check failed: ${error.message}")
                emit(AppUpdateResult.NotAvailable)
            }

    /** Starts the download. The result arrives back through the same flow as [AppUpdateResult]. */
    fun start(result: AppUpdateResult.Available, activity: Activity) {
        result.startFlexibleUpdate(activity, FLEXIBLE_UPDATE_REQUEST)
    }

    /** Restarts into the downloaded build. Call this from the "Restart" action, never on its own. */
    suspend fun install(result: AppUpdateResult.Downloaded) {
        result.completeUpdate()
    }

    private companion object {
        const val FLEXIBLE_UPDATE_REQUEST = 4711
    }
}

/** Asks for a Play Store rating, at a moment the user might plausibly say yes. */
@Singleton
class ReviewPrompt @Inject constructor(
    @ApplicationContext private val context: Context,
    @SettingsDataStore private val dataStore: DataStore<Preferences>,
) {

    /** Call once per launch. Records the launch, and asks only when the policy says to. */
    suspend fun onLaunch(activity: Activity) {
        val preferences = dataStore.data.catch { emit(emptyPreferences()) }.first()
        val launches = (preferences[LAUNCHES] ?: 0) + 1
        val lastAsked = preferences[LAST_ASKED] ?: 0L
        val now = System.currentTimeMillis()

        dataStore.edit { it[LAUNCHES] = launches }

        if (launches < LAUNCHES_BEFORE_ASKING) return
        if (now - lastAsked < DAYS_BETWEEN_ASKING * MILLIS_PER_DAY) return

        // Recorded before the flow: Play never reports the outcome, so recording after would re-ask
        // on failure.
        dataStore.edit { it[LAST_ASKED] = now }

        runCatching {
            val manager = ReviewManagerFactory.create(context)
            val info = manager.requestReview()
            manager.launchReview(activity, info)
        }.onFailure { AppLogger.w("Review prompt unavailable: ${it.message}") }
    }

    private companion object {
        const val LAUNCHES_BEFORE_ASKING = 5
        const val DAYS_BETWEEN_ASKING = 90
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        val LAUNCHES = intPreferencesKey("play_launch_count")
        val LAST_ASKED = longPreferencesKey("play_review_last_asked")
    }
}
