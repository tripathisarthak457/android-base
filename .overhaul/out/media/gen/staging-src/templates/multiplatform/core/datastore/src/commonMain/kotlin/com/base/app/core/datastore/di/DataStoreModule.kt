package com.base.app.core.datastore.di

import com.base.app.core.datastore.preferencesFile
import com.base.app.core.datastore.settings.AppSettingsStore
import org.koin.dsl.module

/** Preferences storage. Needs an `AppDispatchers` and an `ApplicationScope` from the app. */
val dataStoreModule = module {
    single { AppSettingsStore(preferencesFile(SETTINGS_FILE, get(), get()), get()) }
}

private const val SETTINGS_FILE = "settings"
