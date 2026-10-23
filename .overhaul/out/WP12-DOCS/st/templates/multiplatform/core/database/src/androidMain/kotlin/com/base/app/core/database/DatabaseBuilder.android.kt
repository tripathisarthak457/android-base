package com.base.app.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = androidContext()
    return Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath(AppDatabase.NAME).absolutePath)
}
