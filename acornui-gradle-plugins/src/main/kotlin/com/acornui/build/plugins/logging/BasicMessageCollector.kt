package com.acornui.build.plugins.logging

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer

/**
 * BasicMessageCollector just outputs errors to System.err and everything else to System.out
 */
class BasicMessageCollector(val logger: Logger) : MessageCollector {

    private var _hasErrors = false

    override fun clear() {
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        if (severity.isError) {
            _hasErrors = true
            logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
        } else {
            logger.debug(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
        }
    }

    override fun hasErrors(): Boolean = _hasErrors
}