package com.base.app.data.profile.di

// <opt:media>
import android.content.Context
// </opt:media>
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
// <opt:media>
import com.base.app.core.common.coroutines.AppDispatchers
// </opt:media>
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.datastore.DataStoreFiles
import com.base.app.data.profile.DefaultProfileRepository
import com.base.app.data.profile.api.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
// <opt:media>
import dagger.hilt.android.qualifiers.ApplicationContext
// </opt:media>
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
// <opt:media>
import java.io.File
// </opt:media>
import javax.inject.Qualifier
import javax.inject.Singleton
// <opt:media>
import kotlinx.coroutines.withContext
// </opt:media>

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ProfileStore
// <opt:media>

/** Where the avatar is copied to: app storage, which no picker grant or cache clean-up can take away. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AvatarDirectory
// </opt:media>

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
        // <opt:media>

        @Provides
        @AvatarDirectory
        fun provideAvatarDirectory(@ApplicationContext context: Context): File =
            File(context.filesDir, "avatar")
        // </opt:media>

        /** The profile is the signed-in person's, so it goes when they sign out. */
        @Provides
        @IntoSet
        fun provideProfileClearing(
            @ProfileStore store: DataStore<Preferences>,
            // <opt:media>
            @AvatarDirectory avatars: File,
            dispatchers: AppDispatchers,
            // </opt:media>
        ): SessionScopedStore = SessionScopedStore {
            store.edit { it.clear() }
            // <opt:media>
            withContext(dispatchers.io) { avatars.deleteRecursively() }
            // </opt:media>
        }
    }
}
