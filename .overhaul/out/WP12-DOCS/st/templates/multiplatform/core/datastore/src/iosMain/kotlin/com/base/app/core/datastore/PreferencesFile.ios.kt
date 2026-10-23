package com.base.app.core.datastore

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.scope.Scope
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// Application Support rather than Documents: Documents is the person's, and shows in the Files app.
@OptIn(ExperimentalForeignApi::class)
internal actual fun Scope.preferencesDirectory(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return checkNotNull(directory?.path) { "The system gave no Application Support directory." }
}
