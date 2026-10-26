package com.example.finalafter.crui2.core.playstore.review

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.finalafter.crui2.core.common.log.Log
import com.example.finalafter.crui2.core.common.time.AppClock
import com.example.finalafter.crui2.core.playstore.di.PlayStoreData
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CancellationException

/**
 * Asks for a Play Store rating once the app has been opened a few times, and not again for months.
 * A launch is counted once per process, not once per resume: coming back from a share sheet is not
 * a launch.
 */
@Singleton
class ReviewPrompt @Inject internal constructor(
    @ApplicationContext private val context: Context,
    @PlayStoreData private val store: DataStore<Preferences>,
    private val clock: AppClock,
) {
    private var counted = false

    /** Counts this launch and asks when the policy says so. Call with the activity in front. */
    internal suspend fun onLaunch(activity: Activity) {
        if (counted) return
        counted = true

        val now = clock.now().toEpochMilliseconds()
        var ask = false
        store.edit { preferences ->
            val launches = (preferences[LAUNCHES] ?: 0) + 1
            preferences[LAUNCHES] = launches
            ask = shouldAskForReview(launches, preferences[LAST_ASKED], now)
            // Recorded before asking: Play never says whether the sheet was shown, so recording
            // after would ask again whenever it silently declined.
            if (ask) preferences[LAST_ASKED] = now
        }
        if (!ask) return

        try {
            val manager = ReviewManagerFactory.create(context)
            manager.launchReview(activity, manager.requestReview())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Log.w("Review prompt unavailable", failure, TAG)
        }
    }

    private companion object {
        const val TAG = "ReviewPrompt"
        val LAUNCHES = intPreferencesKey("launch_count")
        val LAST_ASKED = longPreferencesKey("review_last_asked")
    }
}

internal const val LAUNCHES_BEFORE_ASKING = 5
internal val TIME_BETWEEN_ASKING = 90.days

/** Play allows only a few prompts a year per person and drops the rest without saying so. */
internal fun shouldAskForReview(launches: Int, lastAskedMillis: Long?, nowMillis: Long): Boolean =
    launches >= LAUNCHES_BEFORE_ASKING &&
        (lastAskedMillis == null || nowMillis - lastAskedMillis >= TIME_BETWEEN_ASKING.inWholeMilliseconds)
