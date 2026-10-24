package com.base.app.core.database

import androidx.room3.RoomDatabase
import org.koin.core.scope.Scope

/** A builder for [AppDatabase]'s file in the app's private storage on each platform. */
internal expect fun Scope.databaseBuilder(): RoomDatabase.Builder<AppDatabase>
