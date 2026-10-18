package com.example.hud.fixture.data.search.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.hud.fixture.core.common.session.SessionScopedStore
import com.example.hud.fixture.core.datastore.DataStoreFiles
import com.example.hud.fixture.data.search.DefaultSearchRepository
import com.example.hud.fixture.data.search.api.SearchRepository
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
internal annotation class RecentSearchesStore

@Module
@InstallIn(SingletonComponent::class)
internal interface SearchDataModule {

    @Binds
    fun bindSearchRepository(impl: DefaultSearchRepository): SearchRepository

    companion object {

        @Provides
        @Singleton
        @RecentSearchesStore
        fun provideRecentSearchesStore(files: DataStoreFiles): DataStore<Preferences> = files.preferences("search")

        /** What someone searched for is theirs, so it goes when they sign out. */
        @Provides
        @IntoSet
        fun provideRecentSearchesClearing(@RecentSearchesStore store: DataStore<Preferences>): SessionScopedStore =
            SessionScopedStore { store.edit { it.clear() } }
    }
}
