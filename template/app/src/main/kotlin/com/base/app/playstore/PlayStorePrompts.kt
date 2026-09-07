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

/**
 * Offers the user a newer build, without taking the app away from them.
 *
 * Flexible rather than immediate: an immediate update blocks the screen behind a full-screen
 * Play dialog and is the right call for a security fix and for nothing else. Flexible downloads
 * in the background and asks to restart when it is ready, so somebody halfway through a form
 * keeps their form.
 *
 * The flow is cold and re-checks on every collection, so collecting it from the Activity's
 * `repeatOnLifecycle` is what makes an app that has been backgrounded for a week notice.
 *
 * Nothing here works on a build that did not come from Play — sideloaded and debug installs get
 * `NotAvailable`, and the emulator without Play Services throws, which is why the flow catches
 * rather than letting a store outage take the Activity down with it.
 */
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

/**
 * Asks for a Play Store rating, at a moment the user might plausibly say yes.
 *
 * The policy is the whole point. Play's own API is silently rate-limited — call it too often and
 * it does nothing at all, with no error — so an app that asks on every launch is an app whose
 * rating prompt never appears, and nobody finds out until they wonder why the review count is
 * flat. Asking after [LAUNCHES_BEFORE_ASKING] launches and at most once every
 * [DAYS_BETWEEN_ASKING] days keeps the requests inside what Play will actually honour.
 *
 * Counting launches rather than sessions or screens because it needs no instrumentation to stay
 * true, and any threshold here is a guess anyway. Move it to something your app knows means
 * "this went well" — an order placed, a workout finished — and the same two calls still apply.
 */
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

        // Recorded before the flow rather than after it: Play never reports whether the sheet was
        // shown or what the user did, by design. Recording on the way out would mean a failure
        // re-asks on the next launch, which is the behaviour the rate limit exists to stop.
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
