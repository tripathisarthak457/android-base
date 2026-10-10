package com.base.app.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.scope.Scope
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// Application Support rather than Documents: Documents is the person's, and shows in the Files app.
@OptIn(ExperimentalForeignApi::class)
internal actual fun Scope.databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val path = checkNotNull(directory?.path) { "The system gave no Application Support directory." }
    return Room.databaseBuilder<AppDatabase>(name = "$path/${AppDatabase.NAME}")
}
