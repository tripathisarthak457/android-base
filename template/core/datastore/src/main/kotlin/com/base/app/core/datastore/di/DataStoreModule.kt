package com.base.app.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.base.app.core.coroutines.ApplicationScope
import com.base.app.core.coroutines.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The signed-in user's data. Cleared wholesale on sign-out.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDataStore

/**
 * Settings that outlive a session — theme, language, onboarding-seen, analytics opt-out.
 *
 * A second file rather than a second key prefix, because sign-out clears the session store by
 * deleting everything in it. Sharing one file would mean either a hand-maintained list of keys to
 * spare — which someone will forget to update — or resetting the user's theme every time they
 * sign out.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStore

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @SessionDataStore
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = create(context, scope, dispatcher, SESSION_FILE)

    @Provides
    @Singleton
    @SettingsDataStore
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = create(context, scope, dispatcher, SETTINGS_FILE)

    private fun create(
        context: Context,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        fileName: String,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        // A file truncated by a crash or a full disk otherwise throws on every read for the rest
        // of the install's life, and the only fix a user has is clearing app data. Starting over
        // empty loses preferences; refusing to start loses the app.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope + dispatcher,
        produceFile = { context.preferencesDataStoreFile(fileName) },
    )

    private const val SESSION_FILE = "session"
    private const val SETTINGS_FILE = "settings"
}
