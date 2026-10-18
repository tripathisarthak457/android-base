package com.example.hud.standard.data.profile.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.hud.standard.core.common.session.SessionScopedStore
import com.example.hud.standard.core.datastore.DataStoreFiles
import com.example.hud.standard.data.profile.DefaultProfileRepository
import com.example.hud.standard.data.profile.api.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ProfileStore

@Module
@InstallIn(SingletonComponent::class)
internal interface ProfileDataModule {

    @Binds
    fun bindProfileRepository(impl: DefaultProfileRepository): ProfileRepository

    companion object {

        @Provides
        @Singleton
        @ProfileStore
        fun provideProfileStore(files: DataStoreFiles): DataStore<Preferences> = files.preferences("profile")

        /** The profile is the signed-in person's, so it goes when they sign out. */
        @Provides
        @IntoSet
        fun provideProfileClearing(
            @ProfileStore store: DataStore<Preferences>,
        ): SessionScopedStore = SessionScopedStore {
            store.edit { it.clear() }
        }
    }
}
