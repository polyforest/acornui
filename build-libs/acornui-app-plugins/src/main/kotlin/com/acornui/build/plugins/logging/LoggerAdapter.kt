package com.acornui.build.plugins.logging

import com.acornui.logging.Log
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import com.acornui.logging.Logger as AcornLogger

object LoggerAdapter {

    fun configure(logger: Logger) {
        if (Log.targets.any { it is AcornToGradleLogger }) return
        Log.targets.clear()
        Log.targets.add(AcornToGradleLogger(logger))
    }
}

private class AcornToGradleLogger(private val logger: Logger) : AcornLogger {
    override var level: Int = AcornLogger.DEBUG

    override fun log(message: Any?, level: Int) {
        val logLevel = when(level) {
            AcornLogger.VERBOSE -> LogLevel.QUIET
            AcornLogger.DEBUG -> LogLevel.DEBUG
            AcornLogger.INFO -> LogLevel.LIFECYCLE
            AcornLogger.WARN -> LogLevel.WARN
            AcornLogger.ERROR -> LogLevel.ERROR
            else -> LogLevel.QUIET
        }
        logger.log(logLevel, message.toString())
    }
}