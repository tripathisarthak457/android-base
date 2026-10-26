package com.example.cmp.data.profile.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.cmp.core.common.coroutines.AppDispatchers
import com.example.cmp.core.common.session.SessionScopedStore
import com.example.cmp.core.datastore.DataStoreFiles
import com.example.cmp.data.profile.DefaultProfileRepository
import com.example.cmp.data.profile.api.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.withContext

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
        fun provideProfileStore(files: DataStoreFiles): DataStore<Preferences> =
            files.preferences("profile")

        @Provides
        @AvatarDirectory
        fun provideAvatarDirectory(@ApplicationContext context: Context): File =
            File(context.filesDir, "avatar")

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
