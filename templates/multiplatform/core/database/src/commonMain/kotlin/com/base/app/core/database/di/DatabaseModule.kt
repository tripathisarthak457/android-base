package com.base.app.core.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.database.AppDatabase
import com.base.app.core.database.databaseBuilder
import org.koin.dsl.module

/**
 * The database, built once on the SQLite that ships with the app rather than the system's, so the
 * same engine and version run on both platforms. Each data module takes its DAO from it.
 */
val databaseModule = module {
    single {
        databaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(get<AppDispatchers>().io)
            .build()
    }
    single { get<AppDatabase>().samples() }
}
