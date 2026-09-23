package com.base.app.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.coroutines.ApplicationScope
import com.base.app.core.coroutines.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Qualifier
import javax.inject.Singleton

/** The signed-in user's data. Cleared wholesale on sign-out. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDataStore

/** Settings that outlive a session — theme, language, onboarding-seen, analytics opt-out. */
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

    /** Any repository can keep per-user data in the session file without registering a store. */
    @Provides
    @IntoSet
    fun provideSessionDataStoreClearing(
        @SessionDataStore dataStore: DataStore<Preferences>,
    ): SessionScopedStore = SessionScopedStore { dataStore.edit { it.clear() } }

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
        // A corrupt file would otherwise throw on every read until the user clears app data.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope + dispatcher,
        produceFile = { context.preferencesDataStoreFile(fileName) },
    )

    private const val SESSION_FILE = "session"
    private const val SETTINGS_FILE = "settings"
}
