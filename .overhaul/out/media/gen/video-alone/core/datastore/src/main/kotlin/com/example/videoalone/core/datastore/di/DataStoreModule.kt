package com.example.videoalone.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.videoalone.core.datastore.DataStoreFiles
import com.example.videoalone.core.datastore.crypto.KeystoreCipher
import com.example.videoalone.core.datastore.crypto.ValueCipher
import com.example.videoalone.core.datastore.session.AuthTokenStore
import com.example.videoalone.core.datastore.session.DataStoreAuthTokenStore
import com.example.videoalone.core.datastore.session.SESSION_TOKENS_FILE
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** Settings that outlive a session: theme, onboarding seen, the analytics opt-out. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStore

/** The session's tokens, in a file of their own. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SessionTokensDataStore

@Module
@InstallIn(SingletonComponent::class)
internal interface DataStoreModule {

    @Binds
    fun bindAuthTokenStore(impl: DataStoreAuthTokenStore): AuthTokenStore

    @Binds
    fun bindValueCipher(impl: KeystoreCipher): ValueCipher

    companion object {

        @Provides
        @Singleton
        @SettingsDataStore
        fun provideSettingsDataStore(files: DataStoreFiles): DataStore<Preferences> = files.preferences("settings")

        @Provides
        @Singleton
        @SessionTokensDataStore
        fun provideSessionTokensDataStore(files: DataStoreFiles): DataStore<Preferences> =
            files.preferences(SESSION_TOKENS_FILE)
    }
}
