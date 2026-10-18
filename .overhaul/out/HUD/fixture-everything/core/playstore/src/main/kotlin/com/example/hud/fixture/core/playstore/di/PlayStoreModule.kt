package com.example.hud.fixture.core.playstore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.hud.fixture.core.datastore.DataStoreFiles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** The launch count, the last review prompt and the update turned down, in a file of their own. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PlayStoreData

@Module
@InstallIn(SingletonComponent::class)
internal object PlayStoreModule {

    @Provides
    @Singleton
    @PlayStoreData
    fun providePlayStoreData(files: DataStoreFiles): DataStore<Preferences> = files.preferences("playstore")
}
