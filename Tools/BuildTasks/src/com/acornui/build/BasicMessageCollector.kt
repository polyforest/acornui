package com.acornui.build

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer

/**
 * BasicMessageCollector just outputs errors to System.err and everything else to System.out
 */
class BasicMessageCollector(val verbose: Boolean = true) : MessageCollector {

	private var _hasErrors = false

	override fun clear() {
	}

	override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
		if (severity.isError) {
			_hasErrors = true
			System.err.println(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
		} else {
			if (verbose || !CompilerMessageSeverity.VERBOSE.contains(severity)) {
				System.out.println(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
			}
		}
	}

	override fun hasErrors(): Boolean = _hasErrors
}