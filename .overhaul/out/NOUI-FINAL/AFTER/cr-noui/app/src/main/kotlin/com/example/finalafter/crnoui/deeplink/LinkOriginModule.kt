package com.example.finalafter.crnoui.deeplink

import android.content.Context
import com.example.finalafter.crnoui.R
import com.example.finalafter.crnoui.core.navigation.LinkOrigin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** The app's own links, read from the same resources the manifest's link filters name. */
@Module
@InstallIn(SingletonComponent::class)
object LinkOriginModule {

    @Provides
    fun provideLinkOrigin(@ApplicationContext context: Context): LinkOrigin = LinkOrigin(
        scheme = context.getString(R.string.deeplink_scheme),
        host = context.getString(R.string.deeplink_host),
    )
}
