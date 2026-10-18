package com.example.hud.fixture.data.profile.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.hud.fixture.core.common.session.SessionScopedStore
import com.example.hud.fixture.core.datastore.DataStoreFiles
import com.example.hud.fixture.data.profile.DefaultProfileRepository
import com.example.hud.fixture.data.profile.api.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton
import android.content.Context
import com.example.hud.fixture.core.common.coroutines.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ProfileStore

/** Where the avatar is copied to: app storage, which no picker grant or cache clean-up can take away. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AvatarDirectory

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

        @Provides
        @AvatarDirectory
        fun provideAvatarDirectory(@ApplicationContext context: Context): File = File(context.filesDir, "avatar")

        /** The profile is the signed-in person's, so it goes when they sign out. */
        @Provides
        @IntoSet
        fun provideProfileClearing(
            @ProfileStore store: DataStore<Preferences>,
            @AvatarDirectory avatars: File,
            dispatchers: AppDispatchers,
        ): SessionScopedStore = SessionScopedStore {
            store.edit { it.clear() }
            withContext(dispatchers.io) { avatars.deleteRecursively() }
        }
    }
}
