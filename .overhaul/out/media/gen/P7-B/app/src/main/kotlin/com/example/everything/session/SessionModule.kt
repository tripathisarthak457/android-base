package com.example.everything.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.everything.core.common.session.SessionController
import com.example.everything.core.common.session.SessionScopedStore
import com.example.everything.core.datastore.DataStoreFiles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Qualifier
import javax.inject.Singleton

/** Whose data is on this device, kept across launches. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionOwnerStore

/** Binds the session implementation to the interface features see. */
@Module
@InstallIn(SingletonComponent::class)
interface SessionModule {

    @Binds
    fun bindSessionController(impl: SessionCoordinator): SessionController

    /** Declared so it can be injected even when nothing has contributed to it yet. */
    @Multibinds
    fun sessionScopedStores(): Set<SessionScopedStore>

    companion object {
        @Provides
        @Singleton
        @SessionOwnerStore
        fun provideSessionOwnerStore(files: DataStoreFiles): DataStore<Preferences> = files.preferences("session_owner")
    }
}
