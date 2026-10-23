package com.base.app.shared.log

import com.base.app.core.common.log.AppLogger
import com.base.app.core.common.log.LogLevel
import platform.Foundation.NSLog

/** Debug builds' log, in Xcode's console and the device log. */
internal object ConsoleLogger : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val line = if (throwable == null) message else "$message\n${throwable.stackTraceToString()}"
        // The text goes in as an argument, never as the format: a % in a message would be read as one.
        NSLog("%@ %@: %@", level.name, tag, line)
    }
}
