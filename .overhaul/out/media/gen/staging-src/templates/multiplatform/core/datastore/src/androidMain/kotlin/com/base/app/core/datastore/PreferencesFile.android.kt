package com.base.app.core.datastore

import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

// Where androidx's own preferencesDataStoreFile puts them, so the backup rules that name it apply.
internal actual fun Scope.preferencesDirectory(): String =
    androidContext().filesDir.resolve("datastore").absolutePath
